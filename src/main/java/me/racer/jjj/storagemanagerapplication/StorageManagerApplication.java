package me.racer.jjj.storagemanagerapplication;

import org.apache.commons.io.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.io.File;
import java.io.IOException;

import static me.racer.jjj.storagemanagerapplication.api.httpServer.initializeHTTPServer;
import static me.racer.jjj.storagemanagerapplication.utils.sql.initsql;

@SpringBootApplication
@RestController
public class StorageManagerApplication {


    public static final String IMG_FILE_PATH = "/cache/images/";

    public static void main(String[] args) {
        initsql("127.0.0.1","3306", "basementstoragemanager","mysql","root","root" );
        initializeHTTPServer("127.0.0.1", 8080);
        //SpringApplication.run(StorageManagerApplication.class, args);
    }

    @GetMapping("/")
    public RedirectView home(RedirectAttributes attributes) {
        return new RedirectView("home");
    }

    @GetMapping("/home")
    public String home(@RequestParam(name="orderby", required=false, defaultValue="expirydate-ASC") String orderby, Model model) {
        model.addAttribute("orderby", orderby);
        return "currentstock";

    }


    @GetMapping("/static/images/{filename}")
    public ResponseEntity<byte[]> getImage(@PathVariable("filename") String filename) {
        byte[] image = new byte[0];
        try {
            image = FileUtils.readFileToByteArray(new File(IMG_FILE_PATH+filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image);
    }


}
