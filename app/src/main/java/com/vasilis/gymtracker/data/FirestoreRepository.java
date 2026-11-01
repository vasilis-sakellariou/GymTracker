package com.vasilis.gymtracker.data;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.vasilis.gymtracker.model.CardioSession;
import com.vasilis.gymtracker.model.Exercise;
import com.vasilis.gymtracker.model.Goal;
import com.vasilis.gymtracker.model.StrengthLog;
import com.vasilis.gymtracker.util.DateUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All communication with the database (Cloud Firestore) happens here.
 *
 * Database layout (each user can only access their own data):
 *
 *   users/{uid}                    ← profile: name, email
 *   users/{uid}/exercises/{id}     ← exercises of the weekly program
 *   users/{uid}/strengthLogs/{id}  ← weight history (chart and goals)
 *   users/{uid}/cardio/{id}        ← cardio workouts
 *   users/{uid}/goals/{id}         ← goals
 *   users/{uid}/dayNotes/{0..6}    ← note for each day of the week
 *
 * Firestore keeps a local copy on the phone, so the app also works offline:
 * changes are sent to the server when the connection comes back.
 */
public class FirestoreRepository {

    private static final String TAG = "FirestoreRepository";

    private static final String USERS = "users";
    private static final String EXERCISES = "exercises";
    private static final String STRENGTH_LOGS = "strengthLogs";
    private static final String CARDIO = "cardio";
    private static final String GOALS = "goals";
    private static final String DAY_NOTES = "dayNotes";

    private static FirestoreRepository instance;

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    // Private: use getInstance() instead (Singleton pattern).
    private FirestoreRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized FirestoreRepository getInstance() {
        if (instance == null) {
            instance = new FirestoreRepository();
        }
        return instance;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** The current user's document: users/{uid}. */
    private DocumentReference userDoc() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("No signed-in user");
        }
        return db.collection(USERS).document(user.getUid());
    }

    /** A sub-collection of the user, e.g. users/{uid}/exercises. */
    private CollectionReference collection(String name) {
        return userDoc().collection(name);
    }

    /**
     * "Listens" to a query: calls the callback now and again every time the data changes.
     * Returns a ListenerRegistration so we can stop listening when the screen closes.
     */
    private <T> ListenerRegistration listen(Query query, Class<T> type, DataCallback<List<T>> callback) {
        return query.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.w(TAG, "Read failed", error);
                return;
            }
            if (snapshot != null) {
                // toObjects: converts every document into a Java object of class "type".
                callback.onResult(snapshot.toObjects(type));
            }
        });
    }

    /** Reads a query ONCE (no live updates). */
    private <T> Task<List<T>> fetch(Query query, Class<T> type) {
        // continueWith: when get() finishes, turn the result into a list of objects.
        return query.get().continueWith(task -> task.getResult().toObjects(type));
    }

    // ------------------------------------------------------------------
    // User profile
    // ------------------------------------------------------------------

    public Task<Void> createUserProfile(String name, String email) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("email", email);
        // serverTimestamp: the time is set by the server, not by the phone.
        data.put("createdAt", FieldValue.serverTimestamp());
        // merge: if the document already exists, only these fields are updated.
        return userDoc().set(data, SetOptions.merge());
    }

    // ------------------------------------------------------------------
    // Day notes
    // ------------------------------------------------------------------

    public void loadDayNote(int dayIndex, DataCallback<String> callback) {
        collection(DAY_NOTES).document(String.valueOf(dayIndex)).get()
                .addOnSuccessListener(doc -> {
                    String note = doc.getString("note");
                    callback.onResult(note == null ? "" : note);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Could not load the day note", e);
                    callback.onResult("");
                });
    }

    public Task<Void> saveDayNote(int dayIndex, String note) {
        Map<String, Object> data = new HashMap<>();
        data.put("note", note);
        return collection(DAY_NOTES).document(String.valueOf(dayIndex)).set(data);
    }

    // ------------------------------------------------------------------
    // Strength exercises
    // ------------------------------------------------------------------

    public ListenerRegistration listenExercises(int dayIndex, DataCallback<List<Exercise>> callback) {
        // whereEqualTo: only the exercises of this day.
        Query query = collection(EXERCISES).whereEqualTo("dayIndex", dayIndex);
        return listen(query, Exercise.class, exercises -> {
            // Sort on the phone by creation time.
            // (Asking the database to sort together with whereEqualTo would require a custom index.)
            exercises.sort((a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
            callback.onResult(exercises);
        });
    }

    /**
     * Adds an exercise AND logs today's weight.
     * The WriteBatch makes both writes together: either both succeed or neither does.
     */
    public Task<Void> addExercise(Exercise exercise) {
        DocumentReference ref = collection(EXERCISES).document(); // new document with a random id
        WriteBatch batch = db.batch();
        batch.set(ref, exercise);
        addWeightLog(batch, ref.getId(), exercise);
        return batch.commit();
    }

    /** Saves the changes of an exercise AND logs today's weight. */
    public Task<Void> updateExercise(Exercise exercise) {
        DocumentReference ref = collection(EXERCISES).document(exercise.getId());
        WriteBatch batch = db.batch();
        batch.set(ref, exercise);
        addWeightLog(batch, exercise.getId(), exercise);
        return batch.commit();
    }

    /**
     * The log id is "exerciseId_todayKey".
     * Saving the same exercise several times on the same day overwrites the same log,
     * so there is one point per day on the chart.
     */
    private void addWeightLog(WriteBatch batch, String exerciseId, Exercise exercise) {
        long today = DateUtils.todayKey();
        StrengthLog log = new StrengthLog(exerciseId, exercise.getName(), today, exercise.getWeightKg());
        batch.set(collection(STRENGTH_LOGS).document(exerciseId + "_" + today), log);
    }

    /** Removes the exercise from the program. Its weight history is kept. */
    public Task<Void> deleteExercise(String exerciseId) {
        return collection(EXERCISES).document(exerciseId).delete();
    }

    public ListenerRegistration listenStrengthLogs(DataCallback<List<StrengthLog>> callback) {
        return listen(collection(STRENGTH_LOGS), StrengthLog.class, callback);
    }

    public Task<List<StrengthLog>> fetchStrengthLogs() {
        return fetch(collection(STRENGTH_LOGS), StrengthLog.class);
    }

    // ------------------------------------------------------------------
    // Cardio
    // ------------------------------------------------------------------

    public ListenerRegistration listenCardio(DataCallback<List<CardioSession>> callback) {
        // Newest first.
        Query query = collection(CARDIO).orderBy("date", Query.Direction.DESCENDING);
        return listen(query, CardioSession.class, callback);
    }

    public Task<List<CardioSession>> fetchCardioSessions() {
        return fetch(collection(CARDIO), CardioSession.class);
    }

    public Task<Void> addCardio(CardioSession session) {
        return collection(CARDIO).document().set(session);
    }

    public Task<Void> updateCardio(CardioSession session) {
        return collection(CARDIO).document(session.getId()).set(session);
    }

    public Task<Void> deleteCardio(String sessionId) {
        return collection(CARDIO).document(sessionId).delete();
    }

    // ------------------------------------------------------------------
    // Goals
    // ------------------------------------------------------------------

    public ListenerRegistration listenGoals(DataCallback<List<Goal>> callback) {
        Query query = collection(GOALS).orderBy("deadline", Query.Direction.ASCENDING);
        return listen(query, Goal.class, callback);
    }

    public Task<List<Goal>> fetchGoals() {
        return fetch(collection(GOALS), Goal.class);
    }

    public Task<Void> addGoal(Goal goal) {
        return collection(GOALS).document().set(goal);
    }

    public Task<Void> deleteGoal(String goalId) {
        return collection(GOALS).document(goalId).delete();
    }
}
