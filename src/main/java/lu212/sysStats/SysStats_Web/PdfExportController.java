package lu212.sysStats.SysStats_Web;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jfree.chart.JFreeChart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lowagie.text.Image;

import jakarta.servlet.http.HttpSession;

@Controller
public class PdfExportController {

	@GetMapping("/pdf-export")
	public String showExportPage(Model model, HttpSession session) {
		Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
		if (loggedIn != null && loggedIn) {
	    File dataDir = new File("data");
	    List<String> servers;

	    if (dataDir.exists() && dataDir.isDirectory()) {
	        File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".txt"));
	        if (files != null) {
	            servers = Arrays.stream(files)
	                    .map(f -> f.getName().replace(".txt", ""))
	                    .collect(Collectors.toList());
	        } else {
	            servers = List.of(); // leere Liste statt null
	        }
	    } else {
	        servers = List.of(); // leere Liste statt null
	    }

	    model.addAttribute("servers", servers);
	    model.addAttribute("activePage", "pdf-export");
	    model.addAttribute("isAdmin", session.getAttribute("isAdmin"));

	    String theme = SysStatsWebApplication.theme;
	    model.addAttribute("theme", theme);

	    return "pdf-export";
		} else {
			return "redirect:/login?error=sessionExpired";
		}
	}

    @PostMapping("/generate-pdf")
    public String generatePdf(@RequestParam List<String> servers,
                              @RequestParam(required = false) List<String> metrics,
                              RedirectAttributes redirectAttributes) throws Exception {

        if (metrics == null || metrics.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Bitte mindestens eine Metrik auswählen.");
            return "redirect:/pdf-export";
        }

        File outputDir = new File("documents");
        if (!outputDir.exists()) outputDir.mkdirs();

        String fileName = "SysStatz_Report_" + System.currentTimeMillis() + ".pdf";
        File outputFile = new File(outputDir, fileName);

        try (OutputStream os = new FileOutputStream(outputFile)) {
            createPdf(os, servers, metrics);
        }

        redirectAttributes.addFlashAttribute("success", "PDF gespeichert unter /documents/" + fileName);
        return "redirect:/pdf-export";
    }

    private void createPdf(OutputStream os, List<String> servers, List<String> metrics) throws Exception {
        com.lowagie.text.Document doc = new com.lowagie.text.Document();
        com.lowagie.text.pdf.PdfWriter writer = com.lowagie.text.pdf.PdfWriter.getInstance(doc, os);
        doc.open();

        for (String server : servers) {
            File file = new File("data", server + ".txt");
            if (!file.exists()) continue;

            List<DataPoint> dataPoints = parseData(file);

            doc.add(new com.lowagie.text.Paragraph("Server: " + server));
            for (String metric : metrics) {
                List<Double> values = dataPoints.stream().map(dp -> dp.getMetric(metric)).collect(Collectors.toList());
                double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);

                doc.add(new com.lowagie.text.Paragraph(metric.toUpperCase() + "-Durchschnitt (7 Tage): " + String.format("%.1f", avg) + "%"));

                // Diagramm
                JFreeChart chart = createChart(dataPoints, metric);
                BufferedImage img = chart.createBufferedImage(500, 300);
                Image image = com.lowagie.text.Image.getInstance(writer, img, 1.0f);
                doc.add(image);
            }
            doc.add(com.lowagie.text.Chunk.NEWLINE);
        }

        doc.close();
    }

    private List<DataPoint> parseData(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        long weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;

        return lines.stream()
                .map(line -> line.split(";"))
                .map(parts -> new DataPoint(Long.parseLong(parts[0]) * 1000L,
                        Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3])))
                .filter(dp -> dp.timestamp >= weekAgo)
                .collect(Collectors.toList());
    }

    private JFreeChart createChart(List<DataPoint> data, String metric) {
        var dataset = new org.jfree.data.time.TimeSeries(metric.toUpperCase());
        for (DataPoint dp : data) {
            dataset.addOrUpdate(new org.jfree.data.time.Second(new Date(dp.timestamp)), dp.getMetric(metric));
        }

        var tsc = new org.jfree.data.time.TimeSeriesCollection();
        tsc.addSeries(dataset);

        return org.jfree.chart.ChartFactory.createTimeSeriesChart(
                metric.toUpperCase() + " Verlauf",
                "Zeit",
                "Wert (%)",
                tsc,
                false, false, false
        );
    }

    static class DataPoint {
        long timestamp;
        double cpu, ram, disk;

        public DataPoint(long ts, double cpu, double ram, double disk) {
            this.timestamp = ts;
            this.cpu = cpu;
            this.ram = ram;
            this.disk = disk;
        }

        double getMetric(String name) {
            return switch (name.toLowerCase()) {
                case "cpu" -> cpu;
                case "ram" -> ram;
                case "disk" -> disk;
                default -> 0;
            };
        }
    }
}
