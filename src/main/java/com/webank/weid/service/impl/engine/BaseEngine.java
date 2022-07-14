

package com.webank.weid.service.impl.engine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;

import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webank.weid.constant.WeIdConstant;
import com.webank.weid.exception.LoadContractException;
import com.webank.weid.service.BaseService;

public abstract class BaseEngine extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(BaseEngine.class);

    public BaseEngine() {
        super();
    }

    /*public BaseEngine(Integer groupId) {
        super(groupId);
    }*/

    private <T> T loadContract(
            String contractAddress,
            //Object credentials,
            Integer groupId,
            CryptoKeyPair cryptoKeyPair,
            Class<T> cls) throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        Object contract;
        Method method = cls.getMethod(
                "load",
                String.class,
                /*getWeb3jClass(),
                credentials.getClass(),
                BigInteger.class,
                BigInteger.class,*/
                Client.class,
                CryptoKeyPair.class
        );
        //Object obj = weServer.getWeb3j();
        contract = method.invoke(
                null,
                contractAddress,
                /*obj,
                credentials,
                WeIdConstant.GAS_PRICE,
                WeIdConstant.GAS_LIMIT,*/
                getClient(groupId),
                cryptoKeyPair
        );
        return (T) contract;
    }

    /**
     * Reload contract.
     *
     * @param contractAddress the contract address
     * @param privateKey the privateKey of the sender
     * @param cls the class
     * @param <T> t
     * @return the contract
     */
    protected <T> T reloadContract(
            String contractAddress,
            String privateKey,
            Class<T> cls) {

        T contract = null;
        try {
            // load contract
            //contract = loadContract(contractAddress, weServer.createCredentials(privateKey), cls);
            contract = loadContract(
                    contractAddress,
                    masterGroupId,
                    getWeServer().createCryptoKeyPair(privateKey),
                    cls
            );
            logger.info(cls.getSimpleName() + " init succ");
        } catch (Exception e) {
            logger.error("load contract :{} failed. Error message is :{}",
                    cls.getSimpleName(), e.getMessage(), e);
            throw new LoadContractException(e);
        }

        if (contract == null) {
            throw new LoadContractException();
        }
        return contract;
    }

    /**
     * Reload contract.
     *
     * @param contractAddress the contract address
     * @param groupId
     * @param privateKey the privateKey of the sender
     * @param cls the class
     * @param <T> t
     * @return the contract
     */
    protected <T> T reloadContract(
            String contractAddress,
            Integer groupId,
            String privateKey,
            Class<T> cls) {

        T contract = null;
        try {
            // load contract
            contract = loadContract(contractAddress, groupId, getWeServer().createCryptoKeyPair(privateKey), cls);
            logger.info(cls.getSimpleName() + " init succ");
        } catch (Exception e) {
            logger.error("load contract :{} failed. Error message is :{}",
                    cls.getSimpleName(), e.getMessage(), e);
            throw new LoadContractException(e);
        }

        if (contract == null) {
            throw new LoadContractException();
        }
        return contract;
    }

    /**
     * Gets the contract service.
     *
     * @param contractAddress the contract address
     * @param cls the class
     * @param <T> t
     * @return the contract service
     */
    protected <T> T getContractService(String contractAddress, Class<T> cls) {

        T contract = null;
        try {
            //contract = loadContract(contractAddress, weServer.getCredentials(), cls);
            contract = loadContract(
                    contractAddress,
                    masterGroupId,
                    getWeServer().getDefaultCryptoKeyPair(),
                    cls
            );
            logger.info(cls.getSimpleName() + " init succ");

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            logger.error("load contract :{} failed. Error message is :{}",
                    cls.getSimpleName(), e.getMessage(), e);
            throw new LoadContractException(e);
        } catch (Exception e) {
            logger.error("load contract Exception:{} failed. Error message is :{}",
                    cls.getSimpleName(), e.getMessage(), e);
            throw new LoadContractException(e);
        }

        if (contract == null) {
            throw new LoadContractException();
        }
        return contract;
    }

    /**
     * Gets the contract service.
     *
     * @param contractAddress the contract address
     * @param groupId
     * @param cls the class
     * @param <T> t
     * @return the contract service
     */
    protected <T> T getContractService(String contractAddress, Integer groupId, Class<T> cls) {

        T contract = null;
        try {
            contract = loadContract(contractAddress, groupId, getWeServer().createCryptoKeyPair(), cls);
            logger.info(cls.getSimpleName() + " init succ");

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            logger.error("load contract :{} failed. Error message is :{}",
                    cls.getSimpleName(), e.getMessage(), e);
            throw new LoadContractException(e);
        } catch (Exception e) {
            logger.error("load contract Exception:{} failed. Error message is :{}",
                    cls.getSimpleName(), e.getMessage(), e);
            throw new LoadContractException(e);
        }

        if (contract == null) {
            throw new LoadContractException();
        }
        return contract;
    }
}
