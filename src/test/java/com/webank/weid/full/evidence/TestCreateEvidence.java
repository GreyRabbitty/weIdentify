

package com.webank.weid.full.evidence;

import com.webank.weid.common.LogUtil;
import com.webank.weid.blockchain.constant.ErrorCode;
import com.webank.weid.full.TestBaseService;
import com.webank.weid.protocol.base.Credential;
import com.webank.weid.protocol.base.CredentialPojo;
import com.webank.weid.protocol.base.EvidenceInfo;
import com.webank.weid.protocol.base.WeIdAuthentication;
import com.webank.weid.protocol.request.TransactionArgs;
import com.webank.weid.protocol.response.CreateWeIdDataResult;
import com.webank.weid.blockchain.protocol.response.ResponseData;
import com.webank.weid.service.local.EvidenceServiceLocal;
import com.webank.weid.util.DataToolUtils;
import com.webank.weid.util.DateUtils;

import com.webank.weid.util.PropertyUtils;
import com.webank.weid.util.WeIdUtils;
import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.crypto.CryptoSuite;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Test CreateEvidence.
 *
 * @author v_wbgyang
 */
public class TestCreateEvidence extends TestBaseService {

    private static final Logger logger = LoggerFactory.getLogger(TestCreateEvidence.class);

    private static volatile Credential credential = null;

    @Override
    public synchronized void testInit() {
        super.testInit();
        if (credential == null) {
            credential = super.createCredential(createCredentialArgs).getCredential();
        }
    }

    /**
     * case1: credential issuer and private key belong the same weId.
     */
    @Test
    public void testCreateEvidence_success() {
        CreateWeIdDataResult tempCreateWeIdResultWithSetAttr =
            super.copyCreateWeId(createWeIdResultWithSetAttr);
        CredentialPojo credential = createCredentialPojo(createCredentialPojoArgs);
        credential.setId(UUID.randomUUID().toString());
        String hash = evidenceService.generateHash(credential).getResult().getHash();
        // Direct call of add - should fail
        ResponseData<Boolean> addResp = evidenceService.addLogByHash(hash, "3.23",
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        Assert.assertFalse(addResp.getResult());
        // Must create evidence and sign first!
        ResponseData<String> response = evidenceService.createEvidence(credential,
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        evidenceService.addLogByHash(hash, "1.23",
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        evidenceService.addLogByHash(hash, "13.15",
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), response.getErrorCode().intValue());
        Assert.assertTrue(!response.getResult().isEmpty());
        ResponseData<EvidenceInfo> eviInfo = evidenceService.getEvidence(hash);
        EvidenceInfo evidenceInfo = eviInfo.getResult();
        Assert.assertTrue(evidenceInfo.getCredentialHash().equalsIgnoreCase(hash));
        String signerWeId = tempCreateWeIdResultWithSetAttr.getWeId();
        Assert.assertTrue(evidenceInfo.getSigners().contains(WeIdUtils.convertWeIdToAddress(signerWeId)));
        Assert.assertEquals(evidenceInfo.getSignInfo().get(WeIdUtils.convertWeIdToAddress(signerWeId)).getLogs().size(), 3);
        Assert.assertTrue(
            evidenceInfo.getSignInfo().get(WeIdUtils.convertWeIdToAddress(signerWeId)).getLogs().get(1).equals("1.23"));
        Assert.assertTrue(
            evidenceInfo.getSignInfo().get(WeIdUtils.convertWeIdToAddress(signerWeId)).getLogs().get(2).equals("13.15"));
        ResponseData<Boolean> resp = evidenceService
            .verifySigner(credential, evidenceInfo, signerWeId);
        Assert.assertTrue(resp.getResult());
    }

    @Test
    public void testEvidenceManipulationFailures() {
        CredentialPojo credential = createCredentialPojo(createCredentialPojoArgs);
        credential.setId(UUID.randomUUID().toString());
        String hash = credential.getHash();
        CreateWeIdDataResult cwdr = createWeIdWithSetAttr();
        // Get another signer here:
        String tempSigner = cwdr.getWeId();
        evidenceService
            .createRawEvidenceWithSpecificSigner(hash, credential.getSignature(), "temp-log",
                DateUtils.getNoMillisecondTimeStamp(), credential.getId(), tempSigner, privateKey);
        ResponseData<EvidenceInfo> eviResp = evidenceService.getEvidence(hash);
        Assert.assertNotNull(eviResp.getResult());
        ResponseData<Boolean> verifyResp = evidenceService
            .verifySigner(credential, eviResp.getResult(), tempSigner);
        // Here it should fail, since: signer's sig ! from privatekey, and issuer != signer
        Assert.assertFalse(verifyResp.getResult());
        Assert.assertEquals(verifyResp.getErrorCode().intValue(),
            ErrorCode.CREDENTIAL_ISSUER_MISMATCH.getCode());

        //----
        credential.setId(UUID.randomUUID().toString());
        credential.setIssuer(tempSigner);
        hash = credential.getHash();
        // Get another signer here:
        evidenceService.createRawEvidenceWithSpecificSigner(hash, credential.getSignature(),
            "temp-log", DateUtils.getNoMillisecondTimeStamp(), credential.getId(), tempSigner,
            privateKey);
        eviResp = evidenceService.getEvidence(hash);
        Assert.assertNotNull(eviResp.getResult());
        verifyResp = evidenceService.verifySigner(credential, eviResp.getResult(), tempSigner);
        // Here it should fail, since: signer's sig ! from privatekey
        Assert.assertFalse(verifyResp.getResult());
        Assert.assertEquals(verifyResp.getErrorCode().intValue(),
            ErrorCode.CREDENTIAL_VERIFY_FAIL.getCode());

        //----
        credential.setId(UUID.randomUUID().toString());
        credential.setIssuer(tempSigner);
        hash = credential.getHash();
        // Get another signer here:
        evidenceService.createRawEvidenceWithSpecificSigner(hash, credential.getSignature(),
            "temp-log", DateUtils.getNoMillisecondTimeStamp(), credential.getId(), tempSigner,
            cwdr.getUserWeIdPrivateKey().getPrivateKey());
        eviResp = evidenceService.getEvidence(hash);
        Assert.assertNotNull(eviResp.getResult());
        verifyResp = evidenceService.verifySigner(credential, eviResp.getResult(), tempSigner);
        // Here it should still fail, since: signer's sig ! from privatekey
        Assert.assertFalse(verifyResp.getResult());
    }

    @Test
    public void testCreateEvidenceDupAndNonExist() {
        CreateWeIdDataResult tempCreateWeIdResultWithSetAttr =
            super.copyCreateWeId(createWeIdResultWithSetAttr);
        CredentialPojo credential = createCredentialPojo(createCredentialPojoArgs);
        credential.setId(UUID.randomUUID().toString());
        String hash = evidenceService.generateHash(credential).getResult().getHash();
        ResponseData<String> createResp1 = evidenceService.createEvidence(credential,
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        ResponseData<String> createResp2 = evidenceService.createEvidence(credential,
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        Assert.assertEquals(createResp1.getResult(), hash);
        Assert.assertEquals(createResp2.getResult(), StringUtils.EMPTY);
        Assert.assertEquals(createResp2.getErrorCode().intValue(),
            ErrorCode.CREDENTIAL_EVIDENCE_ALREADY_EXISTS.getCode());
        createResp2 = evidenceService.createEvidenceWithLogAndCustomKey(credential,
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey(), "a", credential.getId());
        Assert.assertEquals(createResp2.getResult(), StringUtils.EMPTY);
        Assert.assertEquals(createResp2.getErrorCode().intValue(),
            ErrorCode.CREDENTIAL_EVIDENCE_ALREADY_EXISTS.getCode());
        credential.setId(UUID.randomUUID().toString());
        ResponseData<Boolean> addResp1 = evidenceService.addLogByHash(credential.getHash(), "a",
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        Assert.assertEquals(addResp1.getResult(), false);
        Assert.assertEquals(addResp1.getErrorCode().intValue(),
            ErrorCode.CREDENTIAL_EVIDENCE_NOT_EXIST.getCode());
        ResponseData<Boolean> addResp2 = evidenceService.addSignatureAndLogByHash(
            credential.getHash(), "a", tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        Assert.assertEquals(addResp2.getResult(), false);
        Assert.assertEquals(addResp2.getErrorCode().intValue(),
            ErrorCode.CREDENTIAL_EVIDENCE_NOT_EXIST.getCode());
    }

    @Test
    public void testCreateEvidence_MultipleSigners() {
        CreateWeIdDataResult tempCreateWeIdResultWithSetAttr =
            super.copyCreateWeId(createWeIdResultWithSetAttr);
        CredentialPojo credential = createCredentialPojo(createCredentialPojoArgs);
        credential.setId(UUID.randomUUID().toString());
        String hash = evidenceService.generateHash(credential).getResult().getHash();
        ResponseData<String> createResp1 = evidenceService.createEvidence(credential,
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        ResponseData<Boolean> addResp1 = evidenceService.addLogByHash(hash, "1.23",
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        ResponseData<Boolean> addResp2 = evidenceService.addLogByHash(hash, "13.15",
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        // Another guy signs
        CreateWeIdDataResult tempCreateWeIdResultWithSetAttr2 = createWeIdWithSetAttr();
        ResponseData<Boolean> createResp2 = evidenceService.addSignatureAndLogByHash(
            credential.getHash(), "empty log", tempCreateWeIdResultWithSetAttr2.getUserWeIdPrivateKey());
        ResponseData<Boolean> addResp3 = evidenceService.addLogByHash(hash, "abc",
            tempCreateWeIdResultWithSetAttr2.getUserWeIdPrivateKey());
        ResponseData<Boolean> addResp4 = evidenceService.addLogByHash(hash, "eef",
            tempCreateWeIdResultWithSetAttr2.getUserWeIdPrivateKey());
        // Now check
        ResponseData<EvidenceInfo> eviInfo = evidenceService.getEvidence(hash);
        EvidenceInfo evidenceInfo = eviInfo.getResult();
        String signer1 = tempCreateWeIdResultWithSetAttr.getWeId();
        String signer2 = tempCreateWeIdResultWithSetAttr2.getWeId();
        Assert.assertEquals(evidenceInfo.getSignInfo().get(WeIdUtils.convertWeIdToAddress(signer1)).getLogs().size(), 3);
        Assert.assertEquals(evidenceInfo.getSignInfo().get(WeIdUtils.convertWeIdToAddress(signer2)).getLogs().size(), 3);
        ResponseData<Boolean> resp = evidenceService
            .verifySigner(credential, evidenceInfo, signer1);
        Assert.assertTrue(resp.getResult());
        resp = evidenceService.verifySigner(credential, evidenceInfo, signer2);
        // here it should be false since signer changed
        Assert.assertFalse(resp.getResult());
    }

    @Test
    public void testCreateEvidence_CustomKeyHappyPath() {
        CreateWeIdDataResult tempCreateWeIdResultWithSetAttr =
            super.copyCreateWeId(createWeIdResultWithSetAttr);
        CredentialPojo credential = createCredentialPojo(createCredentialPojoArgs);
        credential.setId(UUID.randomUUID().toString());
        String hash = evidenceService.generateHash(credential).getResult().getHash();
        String credId = credential.getId();
        ResponseData<String> cresp = evidenceService.createEvidenceWithLogAndCustomKey(
            credential,
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey(),
            "Ironman",
            hash
        );
        Assert.assertTrue(StringUtils.isEmpty(cresp.getResult()));
        evidenceService.createEvidenceWithLogAndCustomKey(
            credential,
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey(),
            "Ironman",
            credId
        );
        ResponseData<EvidenceInfo> eviHash = evidenceService.getEvidence(hash);
        EvidenceInfo evi1 = eviHash.getResult();
        ResponseData<EvidenceInfo> eviCustomKey = evidenceService.getEvidenceByCustomKey(credId);
        EvidenceInfo evi2 = eviCustomKey.getResult();
        Assert.assertEquals(evi1.getSigners(), evi2.getSigners());
        Assert.assertEquals(evi1.getSignatures(), evi2.getSignatures());
        String signer = tempCreateWeIdResultWithSetAttr.getWeId();
        Assert.assertEquals(evi1.getSignInfo().get(WeIdUtils.convertWeIdToAddress(signer)).getLogs(),
            evi2.getSignInfo().get(WeIdUtils.convertWeIdToAddress(signer)).getLogs());
        evidenceService.addLogByHash(hash, "Insane",
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        evidenceService.addLogByCustomKey(null, credId, "Difficult",
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        evi2 = evidenceService.getEvidenceByCustomKey(credId).getResult();
        Assert.assertEquals(evi2.getSignInfo().get(WeIdUtils.convertWeIdToAddress(signer)).getLogs().size(), 3);
        List<String> list = Arrays.asList("Ironman", "Insane", "Difficult");
        Assert.assertEquals(evi2.getSignInfo().get(WeIdUtils.convertWeIdToAddress(signer)).getLogs(), list);
    }

    @Test
    public void testCreateEvidence_CustomKeyMultiSignerMultiTimes() {
        CredentialPojo credential = createCredentialPojo(createCredentialPojoArgs);
        Assert.assertNotNull(credential);
        credential.setId(UUID.randomUUID().toString());
        String credId = credential.getId();
        List<String> list = new ArrayList<>();
        String log = "X:112.5,Y:97.6";
        list.add(log);
        CreateWeIdDataResult tempCreateWeIdResultWithSetAttr =
            super.copyCreateWeId(createWeIdResultWithSetAttr);
        ResponseData<String> resp = evidenceService.createEvidenceWithLogAndCustomKey(
            credential,
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey(),
            log,
            credId
        );
        Assert.assertTrue(!StringUtils.isEmpty(resp.getResult()));
        log = "X:122.5,Y:94.3";
        list.add(log);
        ResponseData<Boolean> addResp = evidenceService.addLogByCustomKey(
            null,
            credId,
            log,
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey()
        );
        Assert.assertTrue(addResp.getResult());
        log = "X:102.5,Y:99.1";
        list.add(log);
        addResp = evidenceService.addLogByCustomKey(
            null,
            credId,
            log,
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey()
        );
        Assert.assertTrue(addResp.getResult());
        log = "X:0,Y:0";
        list.add(log);
        addResp = evidenceService.addLogByHash(credential.getHash(), log,
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        Assert.assertTrue(addResp.getResult());
        CreateWeIdDataResult tempCreateWeIdResultWithSetAttr2 = createWeIdWithSetAttr();
        addResp = evidenceService.addLogByCustomKey(
            null,
            credId,
            "Age:11",
            tempCreateWeIdResultWithSetAttr2.getUserWeIdPrivateKey()
        );
        Assert.assertTrue(addResp.getResult());
        addResp = evidenceService.addSignatureAndLogByCustomKey(
            null,
            credId,
            "Age:22",
            tempCreateWeIdResultWithSetAttr2.getUserWeIdPrivateKey()
        );
        Assert.assertTrue(addResp.getResult());
        addResp = evidenceService.addSignatureAndLogByCustomKey(
            null,
            credId,
            "Age:33",
            tempCreateWeIdResultWithSetAttr2.getUserWeIdPrivateKey()
        );
        Assert.assertTrue(addResp.getResult());
        addResp = evidenceService.addLogByCustomKey(
            null,
            credId,
            "Age:44",
            tempCreateWeIdResultWithSetAttr2.getUserWeIdPrivateKey()
        );
        Assert.assertTrue(addResp.getResult());
        ResponseData<EvidenceInfo> eviCustomKey = evidenceService.getEvidenceByCustomKey(credId);
        EvidenceInfo evi = eviCustomKey.getResult();
        String signer = tempCreateWeIdResultWithSetAttr.getWeId();
        Assert.assertNotNull(evi.getSignInfo());
        Assert.assertNotNull(evi.getSignInfo().get(WeIdUtils.convertWeIdToAddress(signer)));
        Assert.assertEquals(evi.getSignInfo().get(WeIdUtils.convertWeIdToAddress(signer)).getLogs(), list);
        String signer2 = tempCreateWeIdResultWithSetAttr2.getWeId();
        Assert.assertTrue(evi.getSignInfo().get(WeIdUtils.convertWeIdToAddress(signer2)).getLogs().contains("Age:22")
            && evi.getSignInfo().get(WeIdUtils.convertWeIdToAddress(signer2)).getLogs().size() == 4);
    }

    @Test
    public void testAddLogsAll() {
        CreateWeIdDataResult tempCreateWeIdResultWithSetAttr =
            super.copyCreateWeId(createWeIdResultWithSetAttr);
        CredentialPojo credential = createCredentialPojo(createCredentialPojoArgs);
        credential.setId(UUID.randomUUID().toString());
        String credId = credential.getId();
        String log = DataToolUtils.serialize(credential);
        evidenceService.createEvidenceWithLogAndCustomKey(
            credential,
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey(),
            log,
            credId
        );
        ResponseData<EvidenceInfo> eviResp = evidenceService.getEvidenceByCustomKey(credId);
        EvidenceInfo evi = eviResp.getResult();
        String signer = tempCreateWeIdResultWithSetAttr.getWeId();
        Assert.assertTrue(evi.getSignInfo().get(WeIdUtils.convertWeIdToAddress(signer)).getLogs().contains(log));
        int length = 50; // Up to 2M can still work
        StringBuffer outputBuffer = new StringBuffer(length);
        for (int i = 0; i < length; i++) {
            outputBuffer.append("a");
        }
        log = outputBuffer.toString();
        evidenceService.addLogByCustomKey(null, credId, log,
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        evi = evidenceService.getEvidenceByCustomKey(credId).getResult();
        Assert.assertNotNull(evi);
        Assert.assertTrue(evi.getSignInfo().get(WeIdUtils.convertWeIdToAddress(signer)).getLogs().contains(log));
    }

    @Test
    public void testBatchCreate() throws Exception {
        List<String> hashValues = new ArrayList<>();
        List<String> signatures = new ArrayList<>();
        List<Long> timestamps = new ArrayList<>();
        List<String> signers = new ArrayList<>();
        List<String> logs = new ArrayList<>();
        List<String> customKeys = new ArrayList<>();
        int batchSize = 100;
        for (int i = 0; i < batchSize; i++) {
            CredentialPojo credential = createCredentialPojo(createCredentialPojoArgs);
            credential.setId(UUID.randomUUID().toString());
            String hash = credential.getHash();
            hashValues.add(credential.getHash());
            signatures.add(DataToolUtils.SigBase64Serialization(DataToolUtils.signToRsvSignature(hash, privateKey)));
            timestamps.add(System.currentTimeMillis());
            signers.add(WeIdUtils.getWeIdFromPrivateKey(privateKey));
            logs.add("test log" + i);
            if (i % 2 == 1) {
                customKeys.add(String.valueOf(System.currentTimeMillis()));
            } else {
                customKeys.add(StringUtils.EMPTY);
            }
        }

        //EvidenceServiceEngine engine = EngineFactory.createEvidenceServiceEngine(com.webank.weid.blockchain.service.fisco.BaseService.masterGroupId);
        com.webank.weid.blockchain.rpc.EvidenceService evidenceBlockchainService;
        if (PropertyUtils.getProperty("deploy.style").equals("blockchain")) {
            evidenceBlockchainService = new com.webank.weid.blockchain.service.impl.EvidenceServiceImpl();
        } else {
            // default database
            evidenceBlockchainService = new EvidenceServiceLocal();
        }
        // raw creation
        Long start = System.currentTimeMillis();
        Long end = System.currentTimeMillis();
        System.out.println("Batch creation w/ size: " + batchSize + " takes time (ms): " + (String
            .valueOf(end - start)));
        List<Boolean> booleans = evidenceBlockchainService
                .batchCreateEvidence(hashValues, signatures, logs, timestamps, signers, privateKey).getResult();
        Assert.assertEquals(booleans.size(), hashValues.size());
        Boolean result = true;
        for (int i = 0; i < booleans.size(); i++) {
            result = result && booleans.get(i);
        }
        Assert.assertTrue(result);

        // test illegal hashs
        List<String> faultyHashValues = new ArrayList<>();
        faultyHashValues.addAll(hashValues);
        faultyHashValues.set(1, null);
        booleans = evidenceBlockchainService
                .batchCreateEvidence(faultyHashValues, signatures, logs, timestamps, signers, privateKey).getResult();
        Assert.assertFalse(booleans.get(1));

        // custom keys (semi filled)
        start = System.currentTimeMillis();
        end = System.currentTimeMillis();
        System.out.println(
            "Batch creation w/ custom keys and size: " + batchSize + " takes time (ms): " + (String
                .valueOf(end - start)));
        booleans = evidenceBlockchainService
                .batchCreateEvidenceWithCustomKey(hashValues, signatures, logs, timestamps, signers,
                        customKeys, privateKey).getResult();
        Assert.assertEquals(booleans.size(), hashValues.size());
        result = true;
        // All hashes already existed, so all fail.
        for (int i = 0; i < booleans.size(); i++) {
            result = result && booleans.get(i).booleanValue();
        }
        Assert.assertFalse(result);

        // Check get
        String hash0 = hashValues.get(0);
        String hash1 = hashValues.get(1);
        String key1 = customKeys.get(1);
        EvidenceInfo evidenceInfo0 = evidenceService.getEvidence(hash0).getResult();
        EvidenceInfo evidenceInfo1 = evidenceService.getEvidence(hash1).getResult();
        Assert.assertNotNull(evidenceInfo0);
        Assert.assertNotNull(evidenceInfo1);
    }

    /**
     * This test can only be invoked when using multi-group with group = 1 and 2.
     */
    public void testBatchCreateMultiGroup() throws IOException {
        int batchSize = 100;
        List<TransactionArgs> transactionArgsList = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            CredentialPojo credential = createCredentialPojo(createCredentialPojoArgs);
            credential.setId(UUID.randomUUID().toString());
            String hash = credential.getHash();
            TransactionArgs args = new TransactionArgs();
            args.setMethod("createEvidence");
            List<String> argList = new ArrayList<>();
            argList.add(credential.getHash());
            argList.add(DataToolUtils.SigBase64Serialization(DataToolUtils.signToRsvSignature(hash, privateKey)));
            argList.add("test log" + i);
            argList.add(DateUtils.getNoMillisecondTimeStampString());
            argList
                    .add(WeIdUtils.getWeIdFromPrivateKey(privateKey));
            if (i % 2 == 1) {
                argList.add("2");
            }
            args.setArgs(String.join(",", argList));
            transactionArgsList.add(args);
        }
        /*if ("2".equals(com.webank.weid.blockchain.service.fisco.BaseService.getVersion())) {
            CryptoKeyPair cryptoKeyPair = com.webank.weid.blockchain.service.fisco.CryptoFisco.cryptoSuite.getKeyPairFactory()
                .createKeyPair(new BigInteger(privateKey));
            for (int i = 0; i < batchSize; i++) {
                CredentialPojo credential = createCredentialPojo(createCredentialPojoArgs);
                credential.setId(UUID.randomUUID().toString());
                String hash = credential.getHash();
                TransactionArgs args = new TransactionArgs();
                args.setMethod("createEvidence");
                List<String> argList = new ArrayList<>();
                argList.add(credential.getHash());
                argList.add(com.webank.weid.blockchain.service.fisco.CryptoFisco.cryptoSuite.sign(hash, cryptoKeyPair).convertToString());
                argList.add("test log" + i);
                argList.add(DateUtils.getNoMillisecondTimeStampString());
                argList
                    .add(WeIdUtils.getWeIdFromPrivateKey(privateKey));
                if (i % 2 == 1) {
                    argList.add("2");
                }
                args.setArgs(String.join(",", argList));
                transactionArgsList.add(args);
            }
        } else {
            CryptoSuite cryptoSuite = new CryptoSuite(com.webank.weid.blockchain.service.fisco.CryptoFisco.cryptoSuite.getCryptoTypeConfig());
            org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair cryptoKeyPair =
                cryptoSuite.getKeyPairFactory()
                .createKeyPair(new BigInteger(privateKey));
            for (int i = 0; i < batchSize; i++) {
                CredentialPojo credential = createCredentialPojo(createCredentialPojoArgs);
                credential.setId(UUID.randomUUID().toString());
                String hash = credential.getHash();
                TransactionArgs args = new TransactionArgs();
                args.setMethod("createEvidence");
                List<String> argList = new ArrayList<>();
                argList.add(credential.getHash());
                argList.add(cryptoSuite.sign(hash, cryptoKeyPair).convertToString());
                argList.add("test log" + i);
                argList.add(DateUtils.getNoMillisecondTimeStampString());
                argList
                    .add(WeIdUtils.getWeIdFromPrivateKey(privateKey));
                if (i % 2 == 1) {
                    argList.add("2");
                }
                args.setArgs(String.join(",", argList));
                transactionArgsList.add(args);
            }
        }*/
        //OffLineBatchTask task = new OffLineBatchTask();
        //task.sendBatchTransaction(transactionArgsList);
    }

    @Test
    public void testRawCreation() {
        CredentialPojo credential = createCredentialPojo(createCredentialPojoArgs);
        credential.setId(UUID.randomUUID().toString());
        String hash = credential.getHash();
        String sig = "testSig";
        String log = "abc";
        String customKey = credential.getId();
        ResponseData<Boolean> resp = evidenceService
            .createRawEvidenceWithCustomKey(hash, sig, log, System.currentTimeMillis(), customKey,
                privateKey);
        Assert.assertTrue(resp.getResult());
        ResponseData<EvidenceInfo> eviResp = evidenceService.getEvidenceByCustomKey(customKey);
        Assert.assertTrue(eviResp.getResult().getSignatures().get(0).equalsIgnoreCase(sig));

        //----
        credential.setId(UUID.randomUUID().toString());
        hash = credential.getHash();
        String signer1 = createWeIdResultWithSetAttr.getWeId();
        resp = evidenceService.createRawEvidenceWithSpecificSigner(hash, sig, log,
            System.currentTimeMillis(), StringUtils.EMPTY, signer1, privateKey);
        Assert.assertTrue(resp.getResult());
        eviResp = evidenceService.getEvidence(hash);
        Assert.assertTrue(hash.equalsIgnoreCase(eviResp.getResult().getCredentialHash()));
        Assert.assertTrue(eviResp.getResult().getSigners().size() == 1);
        Assert.assertTrue(eviResp.getResult().getSigners().get(0).equalsIgnoreCase(WeIdUtils.convertWeIdToAddress(signer1)));

        //----
        credential.setId(UUID.randomUUID().toString());
        hash = credential.getHash();
        resp = evidenceService.createRawEvidenceWithSpecificSigner(hash, sig, log,
            System.currentTimeMillis(), credential.getId(), signer1, privateKey);
        Assert.assertTrue(resp.getResult());
        eviResp = evidenceService.getEvidence(hash);
        ResponseData<EvidenceInfo> eviResp2 =
            evidenceService.getEvidenceByCustomKey(credential.getId());
        Assert.assertEquals(eviResp.getResult().getCredentialHash(),
            eviResp2.getResult().getCredentialHash());
        Assert.assertTrue(hash.equalsIgnoreCase(eviResp.getResult().getCredentialHash()));
        Assert.assertTrue(eviResp.getResult().getSigners().size() == 1);
        Assert.assertTrue(eviResp.getResult().getSigners().get(0).equalsIgnoreCase(WeIdUtils.convertWeIdToAddress(signer1)));
    }

    /**
     * Test status: revoked.
     */
    @Test
    public void testSetRevokeStatus() {
        CreateWeIdDataResult tempCreateWeIdResultWithSetAttr = createWeId();
        CredentialPojo credential = createCredentialPojo(createCredentialPojoArgs);
        credential.setId(UUID.randomUUID().toString());
        String hash = evidenceService.generateHash(credential).getResult().getHash();
        ResponseData<String> createResp1 = evidenceService.createEvidence(credential,
            tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        WeIdAuthentication weIdAuthentication = new WeIdAuthentication();
        weIdAuthentication
            .setWeIdPrivateKey(tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        ResponseData<Boolean> revokeResp = evidenceService.revoke(credential, weIdAuthentication);
        Assert.assertTrue(revokeResp.getResult());
        ResponseData<EvidenceInfo> getResp = evidenceService.getEvidence(credential.getHash());
        Assert.assertNotNull(getResp.getResult());
        Assert.assertTrue(getResp.getResult().getSignInfo()
            .get(WeIdUtils.convertWeIdToAddress(tempCreateWeIdResultWithSetAttr.getWeId())).getRevoked());
        revokeResp = evidenceService.unRevoke(credential, weIdAuthentication);
        getResp = evidenceService.getEvidence(credential.getHash());
        Assert.assertFalse(getResp.getResult().getSignInfo()
            .get(WeIdUtils.convertWeIdToAddress(tempCreateWeIdResultWithSetAttr.getWeId())).getRevoked());
        revokeResp = evidenceService.revoke(credential, weIdAuthentication);
        getResp = evidenceService.getEvidence(credential.getHash());
        Assert.assertTrue(getResp.getResult().getSignInfo()
            .get(WeIdUtils.convertWeIdToAddress(tempCreateWeIdResultWithSetAttr.getWeId())).getRevoked());
        EvidenceInfo evidenceInfo = getResp.getResult();
        Assert.assertFalse(evidenceService.isRevoked(
            evidenceInfo, createWeIdResultWithSetAttr.getWeId()).getResult());
        Assert.assertTrue(evidenceService.isRevoked(
            evidenceInfo, tempCreateWeIdResultWithSetAttr.getWeId()).getResult());
    }

    /**
     * case3: weIdPrivateKey is null.
     */
    @Test
    public void testCreateEvidence_priKeyNull() {
        ResponseData<String> response = evidenceService
            .createEvidence(credential, null);
        LogUtil.info(logger, "createEvidence", response);

        Assert.assertEquals(
            ErrorCode.WEID_PRIVATEKEY_INVALID.getCode(),
            response.getErrorCode().intValue());
        Assert.assertFalse(!response.getResult().isEmpty());
    }

    /**
     * case5: privateKey is null.
     */
    @Test
    public void testCreateEvidence_privateKeyNull() {
        CreateWeIdDataResult tempCreateWeIdResultWithSetAttr =
            super.copyCreateWeId(createWeIdResultWithSetAttr);
        tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey().setPrivateKey(null);

        ResponseData<String> response = evidenceService
            .createEvidence(credential, tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        LogUtil.info(logger, "createEvidence", response);
        Assert.assertEquals(
            ErrorCode.WEID_PRIVATEKEY_INVALID.getCode(),
            response.getErrorCode().intValue());
        Assert.assertFalse(!response.getResult().isEmpty());
    }

    /**
     * case6: privateKey is xxxxx.
     */
    @Test
    public void testCreateEvidenceCase05() {
        CreateWeIdDataResult tempCreateWeIdResultWithSetAttr =
            super.copyCreateWeId(createWeIdResultWithSetAttr);
        tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey().setPrivateKey("xxxxx");

        ResponseData<String> response = evidenceService
            .createEvidence(credential, tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        LogUtil.info(logger, "createEvidence", response);
        Assert.assertEquals(
            ErrorCode.WEID_PRIVATEKEY_INVALID.getCode(),
            response.getErrorCode().intValue());
        Assert.assertFalse(!response.getResult().isEmpty());
    }

    /**
     * case6: privateKey is blank.
     */
    @Test
    public void testCreateEvidence_priKeyBlank() {
        CreateWeIdDataResult tempCreateWeIdResultWithSetAttr =
            super.copyCreateWeId(createWeIdResultWithSetAttr);
        tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey().setPrivateKey("");

        ResponseData<String> response = evidenceService
            .createEvidence(credential, tempCreateWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        LogUtil.info(logger, "createEvidence", response);
        Assert.assertEquals(
            ErrorCode.WEID_PRIVATEKEY_INVALID.getCode(),
            response.getErrorCode().intValue());
        Assert.assertFalse(!response.getResult().isEmpty());
    }

    /**
     * Case: generate hash value.
     */
    @Test
    public void testGenerateHash() throws Exception {
        // Credential and CredentialPojo
        Assert.assertTrue(evidenceService.generateHash(credential).getResult().getHash()
            .equalsIgnoreCase(credential.getHash()));
        Assert.assertTrue(evidenceService.generateHash(credentialPojo).getResult().getHash()
            .equalsIgnoreCase(credentialPojo.getHash()));
        Assert.assertTrue(evidenceService.generateHash(selectiveCredentialPojo).getResult()
            .getHash().equalsIgnoreCase(selectiveCredentialPojo.getHash()));
        // Test file
        String path = TestCreateEvidence.class
                .getClassLoader().getResource("test-template.pdf").getPath();
        File file = new File(path);
        String fileHash = evidenceService.generateHash(file).getResult().getHash();
        Assert.assertFalse(StringUtils.isEmpty(fileHash));
        // Support GBK and UTF-8 encoding here - they will yield different hash values, though
        path = TestCreateEvidence.class.getClassLoader().getResource("org1.txt").getPath();
        file = new File(path);
        fileHash = evidenceService.generateHash(file).getResult().getHash();
        Assert.assertFalse(StringUtils.isEmpty(fileHash));
        path = TestCreateEvidence.class.getClassLoader().getResource("test-hash-pic.png").getPath();
        file = new File(path);
        fileHash = evidenceService.generateHash(file).getResult().getHash();
        Assert.assertFalse(StringUtils.isEmpty(fileHash));
        // Non-existent file - uncreated with createNewFile()
        file = new File("non-existent.tmp");
        Assert.assertNull(evidenceService.generateHash(file).getResult());
        Assert.assertNull(evidenceService.generateHash(StringUtils.EMPTY).getResult());
        Assert.assertFalse(StringUtils.isEmpty(
            evidenceService.generateHash("10000").getResult().getHash()));
        Assert.assertNull(evidenceService.generateHash(createCredentialArgs).getResult());
    }
}
