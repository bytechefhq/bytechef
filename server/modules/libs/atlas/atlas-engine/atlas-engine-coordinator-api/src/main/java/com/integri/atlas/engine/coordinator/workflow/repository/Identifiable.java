package com.integri.atlas.engine.coordinator.workflow.repository;

import java.io.IOException;
import java.io.InputStream;

public interface Identifiable {
    String getId();

    InputStream getInputStream() throws IOException;
}
