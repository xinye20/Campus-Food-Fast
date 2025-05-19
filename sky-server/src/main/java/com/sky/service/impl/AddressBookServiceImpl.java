package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AddressBookServiceImpl implements AddressBookService {

    @Autowired
    private AddressBookMapper addressBookMapper;

    /**
     * 查询当前用户所有地址
     *
     * @return
     */
    public List<AddressBook> list() {
        AddressBook addressBook = new AddressBook();
        addressBook.setUserId(BaseContext.getCurrentId());
        return addressBookMapper.list(addressBook);
    }

    /**
     * 新增地址
     *
     * @param addressBook
     */
    public void add(AddressBook addressBook) {
        // 补全地址信息
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBookMapper.add(addressBook);
    }

    /**
     * 设置默认地址
     *
     * @param addressBook
     */
    public void setDefault(AddressBook addressBook) {
        // 将该用户下的默认地址取消
        addressBook.setIsDefault(StatusConstant.DISABLE);
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBookMapper.cleanDefaultByUserId(addressBook);

        // 将指定地址设为默认地址
        addressBook.setIsDefault(StatusConstant.ENABLE);
        addressBookMapper.update(addressBook);
    }

    /**
     * 获取当前用户默认地址
     *
     * @return
     */
    public AddressBook getDefault() {
        // 补全用户id和地址状态
        AddressBook addressBook = new AddressBook();
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(StatusConstant.ENABLE);

        List<AddressBook> list = addressBookMapper.list(addressBook);
        // 判断是否查询到结果
        if (list != null && list.size() > 0) {
            return list.get(0);
        } else {
            return null;
        }

    }

    /**
     * 根据id查询地址
     *
     * @param id
     * @return
     */
    public AddressBook getById(Long id) {
        AddressBook addressBook = new AddressBook();
        addressBook.setId(id);
        List<AddressBook> list = addressBookMapper.list(addressBook);
        return list.get(0);
    }

    /**
     * 根据id修改地址
     *
     * @param addressBook
     */
    public void update(AddressBook addressBook) {
        addressBookMapper.update(addressBook);
    }

    /**
     * 根据id删除地址
     *
     * @param id
     */
    public void deleteById(Long id) {
        addressBookMapper.deleteById(id);
    }
}
