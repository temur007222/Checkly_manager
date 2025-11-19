import {onSchedule} from "firebase-functions/v2/scheduler";
import * as admin from "firebase-admin";

admin.initializeApp();

export const updateAvailableTasks = onSchedule(
  "every 1 minutes",
  async (_event) => {
    const now = admin.firestore.Timestamp.now();

    const tasksSnapshot = await admin.firestore()
      .collectionGroup("tasks")
      .where("status", "==", "NOT_YET_AVAILABLE")
      .get();

    const batch = admin.firestore().batch();

    tasksSnapshot.forEach((doc) => {
      const taskData = doc.data();
      if (
        taskData.availableTime &&
        taskData.availableTime.toMillis() <= now.toMillis()
      ) {
        batch.update(doc.ref, {status: "AVAILABLE"});
      }
    });

    await batch.commit();

    console.log(
      "Updated tasks to AVAILABLE where appropriate."
    );
  }
);
