package all.service;

import all.common.CustomException;
import all.pojo.Category;
import com.baomidou.mybatisplus.extension.service.IService;

public interface CategoryService extends IService<Category> {
    void remove(Long ids) throws CustomException;
}
