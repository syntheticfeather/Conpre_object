package gcc.pra.controller;

import java.io.File;
import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import gcc.pra.pojo.Result;

@RestController
@RequestMapping("/upload")
public class FileUploadController {

    @PostMapping
    public Result<String> upload(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        fileName = UUID.randomUUID().toString() + fileName.substring(fileName.lastIndexOf("."));
        file.transferTo(new File("d:\\Study\\Conpre_object\\BackEnd\\pra\\src\\main\\resources\\static\\uploads\\" + fileName));
        return Result.success("File uploaded successfully");
    } 

}
