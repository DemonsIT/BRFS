
package com.bonree.brfs.schedulers.jobs.biz;

import java.util.List;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.UnableToInterruptJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bonree.brfs.common.utils.BrStringUtils;
import com.bonree.brfs.common.utils.FileUtils;
import com.bonree.brfs.common.utils.JsonUtils;
import com.bonree.brfs.common.utils.TimeUtils;
import com.bonree.brfs.schedulers.utils.JobDataMapConstract;
import com.bonree.brfs.schedulers.utils.LocalFileUtils;
import com.bonree.brfs.schedulers.task.model.AtomTaskModel;
import com.bonree.brfs.schedulers.task.model.AtomTaskResultModel;
import com.bonree.brfs.schedulers.task.model.BatchAtomModel;
import com.bonree.brfs.schedulers.task.model.TaskResultModel;
import com.bonree.brfs.schedulers.task.operation.impl.QuartzOperationStateWithZKTask;
import com.bonree.brfs.schedulers.utils.TaskStateLifeContral;

/******************************************************************************
 * 版权信息：北京博睿宏远数据科技股份有限公司
 * Copyright: Copyright (c) 2007北京博睿宏远数据科技股份有限公司,Inc.All Rights Reserved.
 * @param <AtomTaskModel>
 * 
 * @date 2018年5月3日 下午4:29:44
 * @Author: <a href=mailto:zhucg@bonree.com>朱成岗</a>
 * @Description:系统删除任务 
 *****************************************************************************
 */
public class SystemDeleteJob extends QuartzOperationStateWithZKTask {
	private static final Logger LOG = LoggerFactory.getLogger("SystemDeleteJob");

	@Override
	public void caughtException(JobExecutionContext context) {
		LOG.info("Error ......   ");
	}

	@Override
	public void interrupt() throws UnableToInterruptJobException {
		LOG.info("interrupt ......   ");
	}

	@Override
	public void operation(JobExecutionContext context) throws Exception {
		LOG.info("----------> system delete work");
		JobDataMap data = context.getJobDetail().getJobDataMap();
		String currentIndex = data.getString(JobDataMapConstract.CURRENT_INDEX);
		String dataPath = data.getString(JobDataMapConstract.DATA_PATH);
		String content = data.getString(currentIndex);
		LOG.info("batch {}", content);
		// 获取当前执行的任务类型
		int taskType = data.getInt(JobDataMapConstract.TASK_TYPE);
		BatchAtomModel batch = JsonUtils.toObject(content, BatchAtomModel.class);
		if (batch == null) {
			LOG.warn("batch data is empty !!!");
			return;
		}

		List<AtomTaskModel> atoms = batch.getAtoms();
		if (atoms == null || atoms.isEmpty()) {
			LOG.warn("atom task is empty !!!");
			return;
		}
		String snName = null;
		TaskResultModel result = new TaskResultModel();
		
		AtomTaskResultModel usrResult = null;
		String path = null;
		for (AtomTaskModel atom : atoms) {
			snName = atom.getStorageName();
			if (BrStringUtils.isEmpty(snName)) {
				LOG.warn("sn is empty !!!");
				continue;
			}
			usrResult = deleteDirs(atom, dataPath);
			if (usrResult == null) {
				continue;
			}
			if (!usrResult.isSuccess()) {
				result.setSuccess(false);
			}
			result.add(usrResult);
		}
		//更新任务状态
		TaskStateLifeContral.updateMapTaskMessage(context, result);
	}
	/**
	 * 概述：封装结果
	 * @param atom
	 * @param dataPath
	 * @return
	 * @user <a href=mailto:zhucg@bonree.com>朱成岗</a>
	 */
	public AtomTaskResultModel deleteDirs(AtomTaskModel atom, String dataPath) {
		AtomTaskResultModel atomResult = null;
		String snName = atom.getStorageName();
		int patitionNum = atom.getPatitionNum();
		long granule = atom.getGranule();
		long startTime = TimeUtils.getMiles(atom.getDataStartTime(), TimeUtils.TIME_MILES_FORMATE);
		long endTime = TimeUtils.getMiles(atom.getDataStopTime(), TimeUtils.TIME_MILES_FORMATE);
		List<String> partDirs = LocalFileUtils.getPartitionDirs(dataPath, snName, patitionNum);
		AtomTaskResultModel atomR = AtomTaskResultModel.getInstance(null, snName, startTime, endTime, "", patitionNum);
		if(partDirs == null || partDirs.isEmpty()) {
			return atomR;
		}
		List<String> deleteDirs = LocalFileUtils.collectTimeDirs(partDirs, startTime, endTime, 0, false);
		if(deleteDirs == null || deleteDirs.isEmpty()) {
			return atomR;
		}
		atomR.setOperationFileCount(deleteDirs.size());
		boolean isSuccess = true;
		for(String deleteDir : deleteDirs) {
			isSuccess = isSuccess && FileUtils.deleteDir(deleteDir, true);
			LOG.info("delete :{} status :{} ",deleteDir, isSuccess);
		}
		atomR.setSuccess(isSuccess);
		return atomR;
	}
}
