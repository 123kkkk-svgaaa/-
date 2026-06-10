// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title 去中心化投票合约
 * @notice 所有投票数据上链存储，不可篡改，公开可验证
 * @dev 混合模式架构 — 写操作直接上链，读操作走后端索引
 */
contract VotingContract {

    // ============ 数据结构 ============

    struct Poll {
        uint256 id;
        address creator;
        string title;
        string description;
        string[] options;
        uint256 startTime;
        uint256 endTime;
    }

    // ============ 状态变量 ============

    /// @notice 所有投票的链上存储
    Poll[] private s_polls;

    /// @notice pollId => (voter => hasVoted) 防重复投票
    mapping(uint256 => mapping(address => bool)) private s_hasVoted;

    /// @notice pollId => (optionIndex => count) 各选项得票数
    mapping(uint256 => mapping(uint256 => uint256)) private s_voteCounts;

    /// @notice 投票总数计数器
    uint256 private s_pollCount;

    // ============ 事件 ============

    /// @notice 新投票创建事件
    /// @param pollId 投票 ID
    /// @param creator 创建者地址
    /// @param endTime 投票截止时间 (Unix 时间戳)
    event PollCreated(
        uint256 indexed pollId,
        address indexed creator,
        uint256 endTime
    );

    /// @notice 投票事件
    /// @param pollId 投票 ID
    /// @param voter 投票者地址
    /// @param optionIndex 选择的选项索引
    event VoteCasted(
        uint256 indexed pollId,
        address indexed voter,
        uint256 optionIndex
    );

    // ============ 核心函数 ============

    /**
     * @notice 创建新的投票
     * @param _title       投票标题
     * @param _description 投票描述
     * @param _options     选项列表 (至少 2 个)
     * @param _duration    投票持续时间 (秒)
     * @return pollId      新创建的投票 ID
     */
    function createPoll(
        string memory _title,
        string memory _description,
        string[] memory _options,
        uint256 _duration
    ) external returns (uint256) {
        require(bytes(_title).length > 0, "Poll: title cannot be empty");
        require(_options.length >= 2, "Poll: at least 2 options");
        require(_duration > 0, "Poll: duration must be > 0");

        uint256 pollId = s_pollCount;
        uint256 startTime = block.timestamp;
        uint256 endTime = startTime + _duration;

        Poll storage newPoll = s_polls.push();
        newPoll.id = pollId;
        newPoll.creator = msg.sender;
        newPoll.title = _title;
        newPoll.description = _description;
        newPoll.options = _options;
        newPoll.startTime = startTime;
        newPoll.endTime = endTime;

        s_pollCount++;

        emit PollCreated(pollId, msg.sender, endTime);

        return pollId;
    }

    /**
     * @notice 为指定投票的选项投票
     * @param _pollId      投票 ID
     * @param _optionIndex 选项索引 (从 0 开始)
     */
    function vote(uint256 _pollId, uint256 _optionIndex) external {
        require(_pollId < s_polls.length, "Poll: poll does not exist");
        require(!s_hasVoted[_pollId][msg.sender], "Poll: already voted");

        Poll storage poll = s_polls[_pollId];
        require(
            block.timestamp >= poll.startTime && block.timestamp <= poll.endTime,
            "Poll: not in voting period"
        );
        require(_optionIndex < poll.options.length, "Poll: invalid option");

        s_hasVoted[_pollId][msg.sender] = true;
        s_voteCounts[_pollId][_optionIndex]++;

        emit VoteCasted(_pollId, msg.sender, _optionIndex);
    }

    // ============ 查询函数 ============

    // 获取投票基本信息 (不含 mapping 字段)
    function getPollInfo(
        uint256 _pollId
    )
        external
        view
        returns (
            uint256 id,
            address creator,
            string memory title,
            string memory description,
            string[] memory options,
            uint256 startTime,
            uint256 endTime
        )
    {
        require(_pollId < s_polls.length, "Poll: poll does not exist");
        Poll storage poll = s_polls[_pollId];
        return (
            poll.id,
            poll.creator,
            poll.title,
            poll.description,
            poll.options,
            poll.startTime,
            poll.endTime
        );
    }

    /**
     * @notice 获取指定投票的票数分布
     * @param _pollId 投票 ID
     * @return counts 每个选项的得票数数组
     */
    function getVoteCounts(
        uint256 _pollId
    ) external view returns (uint256[] memory) {
        require(_pollId < s_polls.length, "Poll: poll does not exist");
        uint256 optLen = s_polls[_pollId].options.length;
        uint256[] memory counts = new uint256[](optLen);
        for (uint256 i = 0; i < optLen; i++) {
            counts[i] = s_voteCounts[_pollId][i];
        }
        return counts;
    }

    /**
     * @notice 查询某地址是否已投票
     * @param _pollId 投票 ID
     * @param _voter  投票者地址
     */
    function getHasVoted(
        uint256 _pollId,
        address _voter
    ) external view returns (bool) {
        return s_hasVoted[_pollId][_voter];
    }

    /**
     * @notice 获取投票总数
     */
    function getPollCount() external view returns (uint256) {
        return s_pollCount;
    }

    /**
     * @notice 判断投票是否在进行中
     * @param _pollId 投票 ID
     */
    function isPollActive(uint256 _pollId) external view returns (bool) {
        require(_pollId < s_polls.length, "Poll: poll does not exist");
        Poll storage poll = s_polls[_pollId];
        return block.timestamp >= poll.startTime
            && block.timestamp <= poll.endTime;
    }
}
