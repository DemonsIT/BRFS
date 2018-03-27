package com.bonree.brfs.disknode.server.handler.impl;

import java.io.File;
import java.io.IOException;

import com.bonree.brfs.disknode.DiskWriterManager;
import com.bonree.brfs.disknode.server.handler.DiskMessage;
import com.bonree.brfs.disknode.server.handler.HandleResult;
import com.bonree.brfs.disknode.server.handler.HandleResultCallback;
import com.bonree.brfs.disknode.server.netty.MessageHandler;

public class DeleteMessageHandler implements MessageHandler<DiskMessage> {
	private DiskWriterManager nodeManager;
	
	public DeleteMessageHandler(DiskWriterManager nodeManager) {
		this.nodeManager = nodeManager;
	}

	@Override
	public void handle(DiskMessage msg, HandleResultCallback callback) {
		HandleResult result = new HandleResult();
		
		try {
			nodeManager.close(msg.getFilePath());
			
			File targetFile = new File(msg.getFilePath());
			targetFile.delete();
			
			result.setSuccess(true);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			callback.completed(result);
		}
	}

}