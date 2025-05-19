package com.sky.controller.user;

import com.sky.constant.MessageConstant;
import com.sky.entity.AddressBook;
import com.sky.result.Result;
import com.sky.service.AddressBookService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/addressBook")
@Slf4j
@Api(tags = "地址簿相关接口")
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    /**
     * 查询当前用户所有地址信息
     *
     * @return
     */
    @ApiOperation("查询当前用户所有地址信息")
    @GetMapping("/list")
    public Result<List<AddressBook>> list() {
        log.info("查询当前用户所有地址");
        List<AddressBook> list = addressBookService.list();
        return Result.success(list);
    }

    /**
     * 新增地址
     *
     * @param addressBook
     * @return
     */
    @ApiOperation("新增地址")
    @PostMapping
    public Result add(@RequestBody AddressBook addressBook) {
        log.info("新增地址信息：{}", addressBook);
        addressBookService.add(addressBook);
        return Result.success();
    }

    /**
     * 设置默认地址
     *
     * @param addressBook
     * @return
     */
    @ApiOperation("设置默认地址")
    @PutMapping("/default")
    public Result setDefault(@RequestBody AddressBook addressBook) {
        log.info("将{}设置为默认地址", addressBook);
        addressBookService.setDefault(addressBook);
        return Result.success();
    }

    /**
     * 获取当前用户默认地址
     *
     * @return
     */
    @ApiOperation("获取当前用户默认地址")
    @GetMapping("/default")
    public Result<AddressBook> getDefault() {
        log.info("获取当前用户默认地址");
        AddressBook addressBook = addressBookService.getDefault();
        if (addressBook != null) {
            return Result.success(addressBook);
        } else {
            return Result.error(MessageConstant.DEFAULT_ADDRESS_IS_NOT_SET);
        }
    }

    /**
     * 根据id查询地址
     *
     * @param id
     * @return
     */
    @ApiOperation("根据id查询地址")
    @GetMapping("/{id}")
    public Result<AddressBook> getById(@PathVariable Long id) {
        log.info("根据id查询地址: {}", id);
        AddressBook addressBook = addressBookService.getById(id);
        return Result.success(addressBook);
    }


    /**
     * 根据id修改地址
     *
     * @param addressBook
     * @return
     */
    @ApiOperation("根据id修改地址")
    @PutMapping
    public Result update(@RequestBody AddressBook addressBook) {
        log.info("根据id修改地址{}", addressBook);
        addressBookService.update(addressBook);
        return Result.success();
    }

    /**
     * 根据id删除地址
     *
     * @param id
     * @return
     */
    @ApiOperation("根据id删除地址")
    @DeleteMapping
    public Result delete(Long id) {
        log.info("根据id删除地址:{}", id);
        addressBookService.deleteById(id);
        return Result.success();
    }
}
