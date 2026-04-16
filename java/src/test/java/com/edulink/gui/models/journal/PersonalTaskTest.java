package com.edulink.gui.models.journal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PersonalTaskTest {

    @Test
    public void testTaskStatus() {
        PersonalTask task = new PersonalTask();
        task.setTitle("Complete Homework");
        task.setCompleted(false);

        assertFalse(task.isCompleted());
        assertEquals("Complete Homework", task.getTitle());

        task.setCompleted(true);
        assertTrue(task.isCompleted());
    }
}
