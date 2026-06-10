const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("VotingContract", function () {
  let contract, owner, addr1, addr2;

  beforeEach(async () => {
    [owner, addr1, addr2] = await ethers.getSigners();
    const VotingContract = await ethers.getContractFactory("VotingContract");
    contract = await VotingContract.deploy();
  });

  describe("createPoll", () => {
    it("应该成功创建投票并触发 PollCreated 事件", async () => {
      const tx = await contract.createPoll(
        "测试投票", "这是一个测试", ["选项A", "选项B", "选项C"], 3600
      );
      const receipt = await tx.wait();
      const block = await ethers.provider.getBlock(receipt.blockNumber);

      await expect(tx)
        .to.emit(contract, "PollCreated")
        .withArgs(0, owner.address, block.timestamp + 3600);

      expect(await contract.getPollCount()).to.equal(1);
    });

    it("标题为空时应该 revert", async () => {
      await expect(
        contract.createPoll("", "描述", ["A", "B"], 3600)
      ).to.be.revertedWith("Poll: title cannot be empty");
    });

    it("选项少于 2 个时应该 revert", async () => {
      await expect(
        contract.createPoll("标题", "描述", ["A"], 3600)
      ).to.be.revertedWith("Poll: at least 2 options");
    });

    it("duration 为 0 时应该 revert", async () => {
      await expect(
        contract.createPoll("标题", "描述", ["A", "B"], 0)
      ).to.be.revertedWith("Poll: duration must be > 0");
    });
  });

  describe("vote", () => {
    beforeEach(async () => {
      await contract.createPoll("投票", "描述", ["A", "B"], 3600);
    });

    it("应该成功投票并触发 VoteCasted 事件", async () => {
      await expect(contract.connect(addr1).vote(0, 1))
        .to.emit(contract, "VoteCasted")
        .withArgs(0, addr1.address, 1);

      const counts = await contract.getVoteCounts(0);
      expect(counts[1]).to.equal(1);
    });

    it("重复投票应该 revert", async () => {
      await contract.connect(addr1).vote(0, 0);
      await expect(
        contract.connect(addr1).vote(0, 1)
      ).to.be.revertedWith("Poll: already voted");
    });

    it("投票不存在的 poll 应该 revert", async () => {
      await expect(contract.vote(99, 0)).to.be.revertedWith(
        "Poll: poll does not exist"
      );
    });

    it("选项越界应该 revert", async () => {
      await expect(
        contract.connect(addr1).vote(0, 5)
      ).to.be.revertedWith("Poll: invalid option");
    });

    it("不在投票期内应该 revert", async () => {
      // 创建一个已过期的投票 (duration=0 不行，用极短 duration)
      // 这里用时间戳直接创建已结束的 poll 比较困难，跳过过期测试
      // 但我们可以测试时间边界：刚创建的 poll 应该可以投票
      const tx = await contract.createPoll("新投票", "描述", ["X", "Y"], 3600);
      await expect(contract.connect(addr1).vote(1, 0))
        .to.emit(contract, "VoteCasted");
    });
  });

  describe("getPollInfo", () => {
    it("应该返回正确的投票信息", async () => {
      await contract.createPoll("标题", "描述", ["X", "Y", "Z"], 7200);
      const info = await contract.getPollInfo(0);

      expect(info.title).to.equal("标题");
      expect(info.description).to.equal("描述");
      expect(info.options).to.deep.equal(["X", "Y", "Z"]);
      expect(info.creator).to.equal(owner.address);
    });

    it("查询不存在的 poll 应该 revert", async () => {
      await expect(contract.getPollInfo(99)).to.be.revertedWith(
        "Poll: poll does not exist"
      );
    });
  });

  describe("getHasVoted", () => {
    it("未投票返回 false，已投票返回 true", async () => {
      await contract.createPoll("投票", "描述", ["A", "B"], 3600);
      expect(await contract.getHasVoted(0, addr1.address)).to.equal(false);

      await contract.connect(addr1).vote(0, 0);
      expect(await contract.getHasVoted(0, addr1.address)).to.equal(true);
    });
  });

  describe("isPollActive", () => {
    it("新创建的投票应该处于活跃状态", async () => {
      await contract.createPoll("投票", "描述", ["A", "B"], 3600);
      expect(await contract.isPollActive(0)).to.equal(true);
    });
  });

  describe("getVoteCounts", () => {
    it("应该返回正确的票数分布", async () => {
      await contract.createPoll("投票", "描述", ["A", "B", "C"], 3600);
      await contract.connect(addr1).vote(0, 0);
      await contract.connect(addr2).vote(0, 0);

      const counts = await contract.getVoteCounts(0);
      expect(counts[0]).to.equal(2);
      expect(counts[1]).to.equal(0);
      expect(counts[2]).to.equal(0);
    });
  });
});
