package me.racer.jjj.storagemanagerapplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class StorageManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorageManagerApplication.class, args);
    }

    @GetMapping("/home")
    public String home() {

        return "";
    }

}
