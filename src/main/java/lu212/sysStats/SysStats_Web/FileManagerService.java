package lu212.sysStats.SysStats_Web;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileManagerService {

    private final Path workingDir = Paths.get("").toAbsolutePath();

    public List<FileInfo> listFiles() throws IOException {
        try (var stream = Files.list(workingDir)) {
            return stream
                .filter(path -> !path.getFileName().toString().endsWith(".jar"))
                .map(path -> new FileInfo(path.getFileName().toString(), Files.isDirectory(path)))
                .sorted(Comparator.comparing(FileInfo::isDirectory).reversed()
                        .thenComparing(FileInfo::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        }
    }

    public void createFolder(String folderName) throws IOException {
        Path newFolder = workingDir.resolve(folderName);
        if (!Files.exists(newFolder)) {
            Files.createDirectory(newFolder);
        }
    }

    public void createFile(String fileName) throws IOException {
        Path newFile = workingDir.resolve(fileName);
        if (!Files.exists(newFile)) {
            Files.createFile(newFile);
        }
    }

    public String readFile(String fileName) throws IOException {
        Path file = workingDir.resolve(fileName);
        if (!Files.exists(file) || Files.isDirectory(file)) return "";
        return Files.readString(file);
    }

    public void saveFile(String fileName, String content) throws IOException {
        Path file = workingDir.resolve(fileName);
        if (!Files.exists(file) || Files.isDirectory(file)) throw new IOException("Datei nicht gefunden");
        Files.writeString(file, content, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public Path getFilePath(String fileName) {
        return workingDir.resolve(fileName);
    }
    
    public void saveUploadedFile(MultipartFile multipartFile) throws IOException {
        Path target = workingDir.resolve(multipartFile.getOriginalFilename()).normalize();
        // Optional: Sicherheit prüfen, dass kein Pfad außerhalb des Basisordners liegt
        if (!target.startsWith(workingDir)) {
            throw new IOException("Ungültiger Dateipfad");
        }
        try (InputStream in = multipartFile.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static class FileInfo {
        private final String name;
        private final boolean directory;

        public FileInfo(String name, boolean directory) {
            this.name = name;
            this.directory = directory;
        }
        public String getName() { return name; }
        public boolean isDirectory() { return directory; }
    }
    
    public void deleteFile(String fileName) throws IOException {
        Path path = getFilePath(fileName);
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                // Optional: Verzeichnis rekursiv löschen oder Fehler werfen
                throw new IOException("Löschen von Ordnern ist nicht erlaubt.");
            } else {
                Files.delete(path);
            }
        } else {
            throw new IOException("Datei nicht gefunden: " + fileName);
        }
    }

    public void renameFile(String oldFileName, String newFileName) throws IOException {
        Path oldPath = getFilePath(oldFileName);
        Path newPath = getFilePath(newFileName);

        if (!Files.exists(oldPath)) {
            throw new IOException("Datei nicht gefunden: " + oldFileName);
        }
        if (Files.exists(newPath)) {
            throw new IOException("Zieldatei existiert bereits: " + newFileName);
        }

        Files.move(oldPath, newPath);
    }
}
