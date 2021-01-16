package com.lab.rain.service;

/**
 * @author Alex
 * @version 1.0
 * @date 2020/12/3 17:19
 */
public interface BuildProjectService {

    boolean buildRinexProject(String rinexFile);

    boolean downloadRinex(String rinexFile);

    boolean updateTable();

    boolean downloadRinexBatch(String year, String doyStart, String doyEnd);

}
