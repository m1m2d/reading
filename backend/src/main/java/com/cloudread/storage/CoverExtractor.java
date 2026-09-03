package com.cloudread.storage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 从电子书文件内部元数据提取封面。
 */
@Component
public class CoverExtractor {

    private static final Logger log = LoggerFactory.getLogger(CoverExtractor.class);

    public byte[] extract(InputStream in, String format) {
        if (in == null) {
            return null;
        }
        try {
            switch (format == null ? "" : format.toLowerCase()) {
                case "pdf":
                    return extractFromPdf(in);
                case "epub":
                    return extractFromEpub(in);
                default:
                    return null;
            }
        } catch (Exception e) {
            log.debug("提取封面失败(format={}): {}", format, e.getMessage());
            return null;
        }
    }

    private byte[] extractFromPdf(InputStream in) throws IOException {
        try (PDDocument document = PDDocument.load(in)) {
            if (document.getNumberOfPages() == 0) {
                return null;
            }
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 60, ImageType.RGB);
            int maxHeight = 800;
            if (image.getHeight() > maxHeight) {
                double scale = (double) maxHeight / image.getHeight();
                int w = (int) (image.getWidth() * scale);
                BufferedImage scaled = new BufferedImage(w, maxHeight, BufferedImage.TYPE_INT_RGB);
                scaled.getGraphics().drawImage(image, 0, 0, w, maxHeight, null);
                image = scaled;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        }
    }

    private byte[] extractFromEpub(InputStream in) throws Exception {
        java.nio.file.Path tmp = null;
        try {
            tmp = java.nio.file.Files.createTempFile("epub-cover-", ".epub");
            java.nio.file.Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            try (ZipFile zip = new ZipFile(tmp.toFile())) {
                String opfPath = findOpfPath(zip);
                if (opfPath == null) {
                    return null;
                }
                ZipEntry opfEntry = zip.getEntry(opfPath);
                if (opfEntry == null) {
                    return null;
                }
                Document opf;
                try (InputStream opfIn = zip.getInputStream(opfEntry)) {
                    opf = parseXml(opfIn);
                }
                String coverId = findCoverId(opf);
                String baseDir = opfPath.contains("/")
                        ? opfPath.substring(0, opfPath.lastIndexOf('/') + 1) : "";
                // 1) 按 OPF 的 cover meta 找封面
                if (coverId != null) {
                    byte[] data = readManifestItem(zip, opf, coverId, baseDir);
                    if (data != null && looksLikeImage(data)) {
                        return data;
                    }
                }
                // 2) 兜底：manifest 中第一个图片资源
                List<String> images = manifestImages(zip, opf, baseDir);
                for (String href : images) {
                    byte[] data = readEntry(zip, href);
                    if (data != null && looksLikeImage(data)) {
                        return data;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("EPUB 封面提取失败: {}", e.getMessage());
            return null;
        } finally {
            if (tmp != null) {
                try {
                    java.nio.file.Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    private String findOpfPath(ZipFile zip) throws Exception {
        ZipEntry container = zip.getEntry("META-INF/container.xml");
        if (container == null) {
            return null;
        }
        try (InputStream in = zip.getInputStream(container)) {
            Document doc = parseXml(in);
            NodeList rootfiles = doc.getElementsByTagName("rootfile");
            for (int i = 0; i < rootfiles.getLength(); i++) {
                Element el = (Element) rootfiles.item(i);
                String path = el.getAttribute("full-path");
                if (path != null && !path.isBlank()) {
                    return path;
                }
            }
        }
        return null;
    }

    private Document parseXml(InputStream in) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(in);
    }

    private String findCoverId(Document opf) {
        NodeList metas = opf.getElementsByTagName("meta");
        for (int i = 0; i < metas.getLength(); i++) {
            Element meta = (Element) metas.item(i);
            if ("cover".equalsIgnoreCase(meta.getAttribute("name"))) {
                String content = meta.getAttribute("content");
                if (content != null && !content.isBlank()) {
                    return content;
                }
            }
        }
        return null;
    }

    private byte[] readManifestItem(ZipFile zip, Document opf, String id, String baseDir) throws IOException {
        NodeList items = opf.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            if (id.equals(item.getAttribute("id"))) {
                String href = item.getAttribute("href");
                return readEntry(zip, resolve(baseDir, href));
            }
        }
        return null;
    }

    private List<String> manifestImages(ZipFile zip, Document opf, String baseDir) {
        List<String> images = new ArrayList<>();
        NodeList items = opf.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            String media = item.getAttribute("media-type");
            if (media != null && (media.startsWith("image/"))) {
                images.add(resolve(baseDir, item.getAttribute("href")));
            }
        }
        return images;
    }

    private byte[] readEntry(ZipFile zip, String path) {
        try {
            ZipEntry entry = zip.getEntry(path);
            if (entry == null) {
                return null;
            }
            try (InputStream in = zip.getInputStream(entry)) {
                return in.readAllBytes();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String resolve(String baseDir, String href) {
        if (href == null || href.isBlank()) {
            return "";
        }
        if (href.startsWith("/")) {
            return href.substring(1);
        }
        String path = baseDir + href;
        java.nio.file.Path normalized = java.nio.file.Paths.get(path.replace('/', java.io.File.separatorChar))
                .normalize();
        return normalized.toString().replace('\\', '/');
    }

    private boolean looksLikeImage(byte[] data) {
        if (data == null || data.length < 8) {
            return false;
        }
        return (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8
                || data[0] == 'G' && data[1] == 'I' && data[2] == 'F'
                || (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G';
    }
}
