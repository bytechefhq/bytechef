/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import com.bytechef.file.storage.service.FileStorageService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AppFileStorageTest {

    private static final String APP_ICONS_DIR = "app_icons";

    @Mock
    private FileStorageService fileStorageService;

    private AppFileStorage appFileStorage;

    @BeforeEach
    void beforeEach() {
        appFileStorage = new AppFileStorageImpl(fileStorageService);
    }

    @Test
    void testStoreIconWritesIntoTheAppIconsDirectory() {
        byte[] content = {
            1, 2, 3
        };
        FileEntry fileEntry = new FileEntry("icon.png", "base64:///app_icons/icon.png");

        when(fileStorageService.storeFileContent(APP_ICONS_DIR, "icon.png", content, true)).thenReturn(fileEntry);

        assertThat(appFileStorage.storeIcon("icon.png", content)).isSameAs(fileEntry);
    }

    @Test
    void testReadIconReadsFromTheAppIconsDirectory() {
        byte[] content = {
            4, 5, 6
        };
        FileEntry fileEntry = new FileEntry("icon.webp", "base64:///app_icons/icon.webp");

        when(fileStorageService.readFileToBytes(APP_ICONS_DIR, fileEntry)).thenReturn(content);

        assertThat(appFileStorage.readIcon(fileEntry)).isEqualTo(content);
    }

    @Test
    void testDeleteIconDeletesFromTheAppIconsDirectory() {
        FileEntry fileEntry = new FileEntry("icon.jpg", "base64:///app_icons/icon.jpg");

        appFileStorage.deleteIcon(fileEntry);

        verify(fileStorageService).deleteFile(APP_ICONS_DIR, fileEntry);
    }

    @Test
    void testStoreIconKeepsTheIconsOfTwoAppsApart() {
        Map<String, byte[]> storedFiles = new HashMap<>();
        AppFileStorage nameKeepingAppFileStorage = new AppFileStorageImpl(nameKeepingFileStorageService(storedFiles));

        byte[] firstContent = {
            1
        };
        byte[] secondContent = {
            2
        };

        FileEntry firstIcon = nameKeepingAppFileStorage.storeIcon("icon.png", firstContent);
        FileEntry secondIcon = nameKeepingAppFileStorage.storeIcon("icon.png", secondContent);

        assertThat(firstIcon.getUrl()).isNotEqualTo(secondIcon.getUrl());
        assertThat(nameKeepingAppFileStorage.readIcon(firstIcon)).isEqualTo(firstContent);
        assertThat(nameKeepingAppFileStorage.readIcon(secondIcon)).isEqualTo(secondContent);
    }

    @Test
    void testStoreIconThenDeletePreviousOfTheSameTypeKeepsTheNewIcon() {
        Map<String, byte[]> storedFiles = new HashMap<>();
        AppFileStorage nameKeepingAppFileStorage = new AppFileStorageImpl(nameKeepingFileStorageService(storedFiles));

        byte[] previousContent = {
            1
        };
        byte[] replacementContent = {
            2
        };

        FileEntry previousIcon = nameKeepingAppFileStorage.storeIcon("icon.png", previousContent);
        FileEntry replacementIcon = nameKeepingAppFileStorage.storeIcon("icon.png", replacementContent);

        nameKeepingAppFileStorage.deleteIcon(previousIcon);

        assertThat(nameKeepingAppFileStorage.readIcon(replacementIcon)).isEqualTo(replacementContent);
        assertThatThrownBy(() -> nameKeepingAppFileStorage.readIcon(previousIcon))
            .isInstanceOf(FileStorageException.class);
    }

    @Test
    void testDeleteIconOfOneAppKeepsTheIconOfAnother() {
        Map<String, byte[]> storedFiles = new HashMap<>();
        AppFileStorage nameKeepingAppFileStorage = new AppFileStorageImpl(nameKeepingFileStorageService(storedFiles));

        byte[] otherContent = {
            2
        };

        FileEntry deletedIcon = nameKeepingAppFileStorage.storeIcon("icon.png", new byte[] {
            1
        });
        FileEntry otherIcon = nameKeepingAppFileStorage.storeIcon("icon.png", otherContent);

        nameKeepingAppFileStorage.deleteIcon(deletedIcon);

        assertThat(storedFiles).hasSize(1);
        assertThat(nameKeepingAppFileStorage.readIcon(otherIcon)).isEqualTo(otherContent);
    }

    private static FileStorageService nameKeepingFileStorageService(Map<String, byte[]> storedFiles) {
        FileStorageService nameKeepingFileStorageService = mock(FileStorageService.class);

        lenient()
            .when(nameKeepingFileStorageService.storeFileContent(anyString(), anyString(), any(byte[].class)))
            .thenAnswer(invocation -> storeAsGiven(
                storedFiles, invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2), false));
        lenient()
            .when(nameKeepingFileStorageService.storeFileContent(
                anyString(), anyString(), any(byte[].class), anyBoolean()))
            .thenAnswer(invocation -> storeAsGiven(
                storedFiles, invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2),
                invocation.getArgument(3)));
        lenient()
            .when(nameKeepingFileStorageService.readFileToBytes(eq(APP_ICONS_DIR), any(FileEntry.class)))
            .thenAnswer(invocation -> {
                FileEntry fileEntry = invocation.getArgument(1);

                byte[] content = storedFiles.get(fileEntry.getUrl());

                if (content == null) {
                    throw new FileStorageException("File %s doesn't exist".formatted(fileEntry.getName()));
                }

                return content;
            });
        lenient()
            .doAnswer(invocation -> {
                FileEntry fileEntry = invocation.getArgument(1);

                storedFiles.remove(fileEntry.getUrl());

                return null;
            })
            .when(nameKeepingFileStorageService)
            .deleteFile(eq(APP_ICONS_DIR), any(FileEntry.class));

        return nameKeepingFileStorageService;
    }

    private static FileEntry storeAsGiven(
        Map<String, byte[]> storedFiles, String directory, String filename, byte[] content,
        boolean generateFilename) {

        String storedName = filename;

        if (generateFilename) {
            storedName = UUID.randomUUID() + filename.substring(filename.lastIndexOf('.'));
        }

        String url = "s3://bucket/tenant/" + directory + "/" + storedName;

        storedFiles.put(url, content);

        return new FileEntry(filename, url);
    }
}
