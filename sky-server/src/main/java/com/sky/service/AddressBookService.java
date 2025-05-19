package com.sky.service;

import com.sky.entity.AddressBook;

import java.util.List;

public interface AddressBookService {
    /**
     * 查询当前用户所有地址
     * @return
     */
    List<AddressBook> list();

    /**
     * 新增地址
     * @param addressBook
     */
    void add(AddressBook addressBook);

    /**
     * 设置默认地址
     * @param id
     */
    void setDefault(AddressBook id);

    /**
     * 获取当前用户默认地址
     * @return
     */
    AddressBook getDefault();

    /**
     * 根据id查询地址
     * @param id
     * @return
     */
    AddressBook getById(Long id);

    /**
     * 根据id修改地址
     * @param addressBook
     */
    void update(AddressBook addressBook);

    /**
     * 根据id删除地址
     * @param id
     */
    void deleteById(Long id);
}
