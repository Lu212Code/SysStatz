package lu212.sysStats.SysStats_Web;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import lu212.sysStats.SysStats_Web.FileManagerService.FileInfo;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
@RequestMapping("/filemanager")
public class FileManagerController {

    private final FileManagerService fileManagerService;

    public FileManagerController(FileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    @GetMapping
    public String fileManagerPage(HttpSession session, Model model) throws IOException {
		Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
		if (loggedIn != null && loggedIn) {
        List<FileInfo> files = fileManagerService.listFiles();
        model.addAttribute("files", files);
		String theme = SysStatsWebApplication.theme;
		model.addAttribute("theme", theme);
        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));
        model.addAttribute("activePage", "filemanager");
        return "filemanager";
		} else {
			return "redirect:/login?error=sessionExpired";
		}
    }

    @PostMapping("/create-folder")
    public String createFolder(@RequestParam String folderName, HttpSession session) throws IOException {
		Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
		if (loggedIn != null && loggedIn) {
        fileManagerService.createFolder(folderName.trim());
        return "redirect:/filemanager";
		} else {
			return "redirect:/login?error=sessionExpired";
		}
    }

    @PostMapping("/create-file")
    public String createFile(@RequestParam String fileName, HttpSession session) throws IOException {
		Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
		if (loggedIn != null && loggedIn) {
        fileManagerService.createFile(fileName.trim());
        return "redirect:/filemanager";
		} else {
			return "redirect:/login?error=sessionExpired";
		}
    }

    @GetMapping("/edit")
    public String editFile(@RequestParam String fileName, Model model, HttpSession session) throws IOException {
		Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
		if (loggedIn != null && loggedIn) {
        String content = fileManagerService.readFile(fileName);
        model.addAttribute("fileName", fileName);
        model.addAttribute("content", content);
        model.addAttribute("activePage", "filemanager");
        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));
		String theme = SysStatsWebApplication.theme;
		model.addAttribute("theme", theme);
        return "filemanager_edit";
		} else {
			return "redirect:/login?error=sessionExpired";
		}
    }

    @PostMapping("/save")
    public String saveFile(@RequestParam String fileName, @RequestParam String content, HttpSession session) throws IOException {
		Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
		if (loggedIn != null && loggedIn) {
        fileManagerService.saveFile(fileName, content);
        return "redirect:/filemanager";
		} else {
			return "redirect:/login?error=sessionExpired";
		}
    }

    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> downloadFile(@RequestParam String fileName, HttpSession session) throws IOException {
        Path file = fileManagerService.getFilePath(fileName);
        if (!Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }

        StreamingResponseBody stream;

        if (Files.isDirectory(file)) {
            // Ordner als ZIP streamen
            stream = out -> {
                try (ZipOutputStream zos = new ZipOutputStream(out)) {
                    zipFolder(file, file.getFileName().toString(), zos);
                }
            };

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + ".zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(stream);
        } else {
            // Datei streamen
            stream = out -> {
                try (InputStream is = Files.newInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            };

            String mimeType = Files.probeContentType(file);
            if (mimeType == null) mimeType = "application/octet-stream";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(mimeType))
                    .body(stream);
        }
    }

    // Hilfsmethode zum ZIP-Archivieren eines Ordners (rekursiv)
    private void zipFolder(Path folder, String baseName, ZipOutputStream zos) throws IOException {
        Files.walk(folder).forEach(path -> {
            try {
                String entryName = baseName + "/" + folder.relativize(path).toString().replace("\\", "/");
                if (Files.isDirectory(path)) {
                    if (!entryName.endsWith("/")) entryName += "/";
                    zos.putNextEntry(new ZipEntry(entryName));
                    zos.closeEntry();
                } else {
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(path, zos);
                    zos.closeEntry();
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
    
    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            // Optional: Fehlermeldung setzen oder einfach zurück
            return "redirect:/filemanager?error=emptyfile";
        }
        fileManagerService.saveUploadedFile(file);
        return "redirect:/filemanager?success=uploaded";
    }
    
    @PostMapping("/delete")
    public String deleteFile(@RequestParam String fileName) throws IOException {
        fileManagerService.deleteFile(fileName.trim());
        return "redirect:/filemanager";
    }

    @PostMapping("/rename")
    public String renameFile(@RequestParam String oldFileName, @RequestParam String newFileName) throws IOException {
        fileManagerService.renameFile(oldFileName.trim(), newFileName.trim());
        return "redirect:/filemanager";
    }
}