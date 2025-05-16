package com.contrast.labs.ai.mcp.contrast.data;

import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.sca.LibraryObservation;

import java.util.List;

public record LibraryLibraryObservation(LibraryExtended library, List<LibraryObservation> libraryObservation) {
}
