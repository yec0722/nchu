package com.atguigu.gmall1213.product.controller;

import com.atguigu.gmall1213.common.result.Result;
import org.apache.commons.io.FilenameUtils;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/product/")
public class FileUploadController {
    @Value("${fileServer.url}")
    private String fileUrl; //  fileUrl=http://192.168.200.128:8080/

    // 获取上传文件！使用springMVC MultipartFile
    // 当图片上传完成之后，回显图片，回显图片本质是：图片路径字符串
    // PostMapping 可以，那么我用这个 @RequestMapping {既能接收get ，也能接收post}
    @RequestMapping("/fileUpload")
    public Result<String> fileUpload(MultipartFile file) throws Exception {
        String path = null;
        // 读取到tracker.conf 项目路径不能有中文或者特符号
        String configFile = this.getClass().getResource("/tracker.conf").getFile();
        if (null != configFile) {
            // 初始化数据
            ClientGlobal.init(configFile);
            // 创建trackerClient,trackerService
            TrackerClient trackerClient = new TrackerClient();
            TrackerServer trackerServer = trackerClient.getConnection();
            // 创建storageClient1
            StorageClient1 storageClient1 = new StorageClient1(trackerServer, null);
            // 准备开始上传文件
            // 获取文件后缀名
            String extName = FilenameUtils.getExtension(file.getOriginalFilename());
            path = storageClient1.upload_appender_file1(file.getBytes(), extName, null);

            //打印文件路径
            System.out.println("文件路径是:\t" + fileUrl + path);
        }
        return Result.ok(fileUrl + path);
    }
}

