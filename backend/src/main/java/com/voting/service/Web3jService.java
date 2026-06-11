package com.voting.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Web3j 服务 — 与链上合约交互的封装层
 */
@Service
public class Web3jService {

    private static final Logger log = LoggerFactory.getLogger(Web3jService.class);

    private final Web3j web3j;

    @Value("${web3j.contract-address}")
    private String contractAddress;

    public Web3jService(Web3j web3j) {
        this.web3j = web3j;
    }

    /**
     * 从链上读取完整投票信息 (含票数)
     */
    public Map<String, Object> getPollFromChain(Long pollId) {
        Function function = new Function(
                "getPollInfo",
                Collections.singletonList(new Uint256(pollId)),
                Arrays.asList(
                        new TypeReference<Uint256>() {},
                        new TypeReference<Address>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<DynamicArray<Utf8String>>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {}
                ));

        List<Type> result = callContract(function);
        if (result == null || result.isEmpty()) {
            log.error("getPollFromChain returned empty for pollId={}", pollId);
            throw new RuntimeException("链上查询失败");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", ((Uint256) result.get(0)).getValue().longValue());
        data.put("creator", ((Address) result.get(1)).getValue());
        data.put("title", ((Utf8String) result.get(2)).getValue());
        data.put("description", ((Utf8String) result.get(3)).getValue());
        @SuppressWarnings("unchecked")
        List<Utf8String> opts = ((DynamicArray<Utf8String>) result.get(4)).getValue();
        data.put("options", opts.stream().map(Utf8String::getValue).collect(Collectors.toList()));
        data.put("startTime", ((Uint256) result.get(5)).getValue().longValue());
        data.put("endTime", ((Uint256) result.get(6)).getValue().longValue());

        // 同时获取票数分布
        data.put("voteCounts", getVoteCountsFromChain(pollId));

        return data;
    }

    /**
     * 从链上获取票数分布
     */
    private List<Long> getVoteCountsFromChain(Long pollId) {
        Function function = new Function(
                "getVoteCounts",
                Collections.singletonList(new Uint256(pollId)),
                Collections.singletonList(
                        new TypeReference<DynamicArray<Uint256>>() {}));

        List<Type> result = callContract(function);
        if (result == null || result.isEmpty()) {
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<Uint256> counts = ((DynamicArray<Uint256>) result.get(0)).getValue();
        return counts.stream().map(c -> c.getValue().longValue()).collect(Collectors.toList());
    }

    /**
     * 执行只读合约调用 (eth_call)
     */
    private List<Type> callContract(Function function) {
        try {
            String encoded = FunctionEncoder.encode(function);
            Transaction tx = Transaction.createEthCallTransaction(
                    "0x0000000000000000000000000000000000000000",
                    contractAddress, encoded);

            EthCall response = web3j.ethCall(tx, DefaultBlockParameterName.LATEST).send();
            if (response.hasError()) {
                log.error("Contract call error: {} (function: {})",
                        response.getError().getMessage(), function.getName());
                throw new RuntimeException("合约调用错误: " + response.getError().getMessage());
            }
            return FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Contract call failed: {} (function: {})", e.getMessage(), function.getName(), e);
            throw new RuntimeException("合约调用失败: " + e.getMessage(), e);
        }
    }
}
