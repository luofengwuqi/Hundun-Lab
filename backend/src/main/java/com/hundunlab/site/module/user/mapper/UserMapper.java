package com.hundunlab.site.module.user.mapper;

import com.hundunlab.site.common.anno.AutoFillTime;
import com.hundunlab.site.common.anno.AutoLog;
import com.hundunlab.site.common.constant.ModuleNameConstant;
import com.hundunlab.site.common.constant.type.OperateTypeConstant;
import com.hundunlab.site.common.enums.AutoFillTimeType;
import com.hundunlab.site.module.user.pojo.Entity.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper {
//    User selectByUserNameAndPassword(LoginRequestDTO l);

    //这里不知道加合不合适(太频繁使用了 先不加了)
//    @AutoLog(operateType = OperateTypeConstant.UPDATE, moduleName = ModuleNameConstant.USER)
    User getUserByUserName(String username);

    @AutoFillTime(AutoFillTimeType.INSERT)
    @AutoLog(operateType = OperateTypeConstant.INSERT, moduleName = ModuleNameConstant.USER)
    void insertUser(User u);

    @AutoFillTime(AutoFillTimeType.UPDATE)
    @AutoLog(operateType = OperateTypeConstant.UPDATE, moduleName = ModuleNameConstant.USER)
    void updateUserByUserName(User u);

    @Delete("delete from users where username = #{username}")
    @AutoLog(operateType = OperateTypeConstant.DELETE, moduleName = ModuleNameConstant.USER)
    void deleteUserByUserName(String username);


    List<User> selectByUserNameList(List<String> userNameList);

    @AutoLog(operateType = OperateTypeConstant.DELETE, moduleName = ModuleNameConstant.USER)
    void deleteUserByUserNameList(List<String> userNameList);

    @Select("Select * from users where phone=#{phone}")
    User selectUserByPhone(String phone);

    @Select("Select * from users where id= #{id}")
    User selectUserById(Long id);

    @Delete("delete from users where id= #{id}")
    void deleteUserById(Long id);
    @Select("Select * from users where email= #{email}")
    User selectUserByEmail(String email);

    Integer checkUserNotExists(String username, String phone, String email);

    @Update("update users set avatar_url=#{objectName} where id=#{currentId}")
    void updateUserAvatarPathById(Long currentId, String objectName);

    @Update("update users set nickname= #{nickname} where id= #{id}")
    void updateNickNameById(String nickname, Long id);

    @Update("update users set email= #{email} where id= #{id}")
    void updateEmailById(String email, Long id);

    @Update("update users set phone= #{phone} where id= #{id}")
    void updatePhoneById(String phone, Long id);

    @Update("update users set password_hash= #{s} where id= #{currentId}")
    void updatePasswordById(String s, Long currentId);
}
