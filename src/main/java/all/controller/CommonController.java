package all.controller;

import all.common.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件上传下载
 */
@RestController
@RequestMapping("/common")
public class CommonController {
    @Value(value = "${reggie.path}")
    private String path;
    /**
     *
     * @param file 名称必须与前端一致，或者使用@RequestParam指定
     * @return
     */
    @PostMapping("/upload")
    public R<String> upload(MultipartFile file){
        //获取文件名
        String originalFilename = file.getOriginalFilename();
        //截取文件名的后缀
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        //使用UUID随机生成文件名,加上文件的后缀
        String fileName = UUID.randomUUID().toString()+suffix;
        //创建basepath
        File dir = new File(path);
        //判断是否存在
        if(!dir.exists()){
            //不存在就创建
            dir.mkdirs();
        }
        //将上传的文件动态存储到path下
        try {
            file.transferTo(new File(path + fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //返回图片名称，以供download方法
        return R.success(fileName);
    }
    /**
     * 文件下载
     */
    @GetMapping("/download")
    public void download(String name, HttpServletResponse response){

        try {
            //输入流，通过输入流读取文件内容
            FileInputStream fileInputStream=new FileInputStream(new File(path+name));
            //输出流，通过输出流将文件写回浏览器，在浏览器展示图片
            ServletOutputStream outputStream=response.getOutputStream();
            //固定写法，设置文件类型是图片
            response.setContentType("image/jpeg");
            //下载图片
            int len=0;
            byte[] bytes=new byte[1024];
            while ((len=fileInputStream.read(bytes))!=-1){
                outputStream.write(bytes,0,len);
                outputStream.flush();
            }

            //关闭资源
            outputStream.close();
            fileInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
