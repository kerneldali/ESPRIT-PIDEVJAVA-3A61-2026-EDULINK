package com.edulink.gui.models.journal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NoteTest {

    @Test
    public void testNoteGettersAndSetters() {
        Note note = new Note();
        note.setId(1);
        note.setTitle("Test Title");
        note.setContent("Test Content");
        note.setNotebookId(10);
        note.setCategoryId(5);

        assertEquals(1, note.getId());
        assertEquals("Test Title", note.getTitle());
        assertEquals("Test Content", note.getContent());
        assertEquals(10, note.getNotebookId());
        assertEquals(5, note.getCategoryId());
    }
}
