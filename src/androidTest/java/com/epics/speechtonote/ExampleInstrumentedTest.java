package com.epics.speechtonote;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        android.content.Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.epics.speechtonote", appContext.getPackageName());
    }
}
