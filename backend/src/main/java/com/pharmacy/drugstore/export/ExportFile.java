package com.pharmacy.drugstore.export;

public record ExportFile(byte[] content, String contentType, String fileName) {}
