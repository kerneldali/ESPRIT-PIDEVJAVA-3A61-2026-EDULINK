package com.edulink.gui.services.journal;

import com.edulink.gui.models.journal.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JournalConformityTest {

    @Test
    public void testArchitectureConformity() {
        // This test ensures that the service layer and model layer are properly instantiated
        NoteService service = new NoteService();
        assertNotNull(service, "NoteService should be instantiable");
        
        Note note = new Note();
        note.setTitle("Conformity Note");
        assertEquals("Conformity Note", note.getTitle());
    }
}
