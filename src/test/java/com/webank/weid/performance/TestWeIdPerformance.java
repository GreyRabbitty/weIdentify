

package com.webank.weid.performance;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webank.weid.BaseTest;
import com.webank.weid.blockchain.constant.ErrorCode;
import com.webank.weid.protocol.base.WeIdDocument;
import com.webank.weid.protocol.request.ServiceArgs;
import com.webank.weid.protocol.response.CreateWeIdDataResult;
import com.webank.weid.blockchain.protocol.response.ResponseData;

/**
 * performance testing.
 *
 * @author v_wbgyang
 */
public class TestWeIdPerformance extends BaseTest {

    private static final Logger logger = LoggerFactory.getLogger(TestWeIdPerformance.class);

    @Test
    public void testGetWeIdDom() throws IOException {

        logger.info("blockNumber:{}", com.webank.weid.blockchain.util.DataToolUtils.getBlockNumber());

        CreateWeIdDataResult weIdResult = this.createWeId();
        logger.info("WeIdentity DID:{}", weIdResult.getWeId());

        int count = 1;
        for (int i = 0; i < count; i++) {
            this.setService(
                weIdResult,
                "driving" + i,
                "https://weidentity.webank.com/endpoint/8377464" + i
            );
        }
        logger.info("blockNumber:{}", com.webank.weid.blockchain.util.DataToolUtils.getBlockNumber());

        long startTime = System.currentTimeMillis();

        WeIdDocument result = this.getWeIdDom(weIdResult.getWeId());
        logger.info(String.valueOf(result.getService().size()));

        long gasTime = System.currentTimeMillis() - startTime;
        logger.info("use time:{}ms", gasTime);

        Assert.assertNotNull(result);
    }

    /**
     * create WeIdentity DID.
     */
    public CreateWeIdDataResult createWeId() {

        // create WeIdentity DID,publicKey,privateKey
        ResponseData<CreateWeIdDataResult> responseCreate = weIdService.createWeId();
        // check result is success
        if (responseCreate.getErrorCode() != ErrorCode.SUCCESS.getCode()) {
            logger.info("createWeId fail :{}", responseCreate.getErrorMessage());
            Assert.assertTrue(false);
        }
        return responseCreate.getResult();
    }

    /**
     * setService.
     */
    public void setService(
        CreateWeIdDataResult createResult,
        String serviceType,
        String serviceEnpoint) {

        // setService for this WeIdentity DID
        ServiceArgs setServiceArgs = new ServiceArgs();
        setServiceArgs.setType(serviceType);
        setServiceArgs.setServiceEndpoint(serviceEnpoint);
        ResponseData<Boolean> responseSetSer = weIdService.setService(createResult.getWeId(),
            setServiceArgs, createResult.getUserWeIdPrivateKey());
        // check is success
        if (responseSetSer.getErrorCode() != ErrorCode.SUCCESS.getCode()
            || !responseSetSer.getResult()) {
            logger.info("setService fail :{}", responseSetSer.getErrorMessage());
            Assert.assertTrue(false);
        }
    }

    /**
     * getWeIdDom.
     */
    public WeIdDocument getWeIdDom(String weId) {

        // get weIdDom
        ResponseData<WeIdDocument> responseResult = weIdService.getWeIdDocument(weId);
        // check result
        if (responseResult.getErrorCode() != ErrorCode.SUCCESS.getCode()
            || responseResult.getResult() == null) {
            logger.info("getWeIdDocument fail :{}", responseResult.getErrorMessage());
            Assert.assertTrue(false);
        }
        return responseResult.getResult();
    }
}
