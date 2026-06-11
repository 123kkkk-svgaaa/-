const pptxgen = require("pptxgenjs");

const pres = new pptxgen();
pres.layout = "LAYOUT_16x9";
pres.author = "Voting DApp Team";
pres.title = "去中心化投票系统 — 课程设计答辩";

// ============ COLOR PALETTE ============
const C = {
  bg:       "0D1117",
  bgCard:   "161B22",
  border:   "30363D",
  blue:     "58A6FF",
  green:    "3FB950",
  orange:   "F78166",
  purple:   "BC8CFF",
  white:    "E6EDF3",
  gray:     "8B949E",
  darkGray: "484F58",
  red:      "F85149",
};

// Factory functions for reusable option objects
const makeShadow = () => ({ type: "outer", blur: 6, offset: 2, angle: 135, color: "000000", opacity: 0.3 });
const makeCardShadow = () => ({ type: "outer", blur: 4, offset: 1, angle: 135, color: "000000", opacity: 0.2 });

// ============ SLIDE 1: COVER ============
{
  const slide = pres.addSlide();
  slide.background = { color: C.bg };

  // Large decorative gradient-like shape top-right
  slide.addShape(pres.shapes.OVAL, {
    x: 7.5, y: -1.5, w: 4.5, h: 4.5,
    fill: { color: C.blue, transparency: 92 }
  });
  slide.addShape(pres.shapes.OVAL, {
    x: 8.2, y: -0.8, w: 3.5, h: 3.5,
    fill: { color: C.purple, transparency: 90 }
  });

  // Chain icon emoji as visual
  slide.addText("⛓️", {
    x: 0.8, y: 1.2, w: 1.2, h: 1.2, fontSize: 52, align: "center"
  });

  // Main title
  slide.addText("去中心化投票系统", {
    x: 0.8, y: 2.4, w: 8.5, h: 1.0,
    fontSize: 44, fontFace: "Arial Black", color: C.white, bold: true, margin: 0
  });

  // Subtitle
  slide.addText("基于以太坊智能合约 + SpringBoot + Vue3 全栈 DApp", {
    x: 0.8, y: 3.3, w: 8.5, h: 0.5,
    fontSize: 16, color: C.blue, margin: 0
  });

  // Bottom info bar
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0, y: 4.9, w: 10, h: 0.725,
    fill: { color: C.bgCard }
  });
  slide.addText([
    { text: "课程设计答辩", options: { fontSize: 14, color: C.white, bold: true, breakLine: true } },
    { text: "2026年6月  |  区块链技术与应用", options: { fontSize: 11, color: C.gray } }
  ], { x: 0.8, y: 4.95, w: 8, h: 0.6, margin: 0 });
}

// ============ SLIDE 2: PROJECT OVERVIEW ============
{
  const slide = pres.addSlide();
  slide.background = { color: C.bg };

  slide.addText("项目背景", {
    x: 0.8, y: 0.3, w: 8, h: 0.7,
    fontSize: 32, fontFace: "Arial Black", color: C.white, bold: true, margin: 0
  });

  // 3 cards in a row
  const cards = [
    { title: "🔍 痛点", desc: "传统投票存在暗箱操作、数据篡改、结果不透明等问题，缺乏公信力" },
    { title: "🎯 目标", desc: "构建链上存储、不可篡改、公开可验证的去中心化投票平台" },
    { title: "💡 方案", desc: "混合架构 — 写操作直接上链保证不可篡改，读操作走后端索引提升体验" }
  ];
  cards.forEach((c, i) => {
    const cx = 0.5 + i * 3.1;
    slide.addShape(pres.shapes.RECTANGLE, {
      x: cx, y: 1.3, w: 2.8, h: 2.6,
      fill: { color: C.bgCard }, shadow: makeCardShadow()
    });
    slide.addText(c.title, {
      x: cx + 0.2, y: 1.45, w: 2.4, h: 0.45,
      fontSize: 17, color: C.blue, bold: true, margin: 0
    });
    slide.addText(c.desc, {
      x: cx + 0.2, y: 2.05, w: 2.4, h: 1.6,
      fontSize: 13, color: C.gray, valign: "top", margin: 0
    });
  });

  // Bottom tagline
  slide.addText("所有投票数据上链存储，结果不可篡改，匿名可追溯，公开可验证", {
    x: 0.8, y: 4.4, w: 8.5, h: 0.5,
    fontSize: 14, color: C.green, italic: true, align: "center", margin: 0
  });
}

// ============ SLIDE 3: ARCHITECTURE ============
{
  const slide = pres.addSlide();
  slide.background = { color: C.bg };

  slide.addText("系统架构", {
    x: 0.8, y: 0.3, w: 8, h: 0.7,
    fontSize: 32, fontFace: "Arial Black", color: C.white, bold: true, margin: 0
  });

  // Three-layer architecture cards
  const layers = [
    { title: "表现层", tech: "Vue 3 + Vite + Element Plus", items: ["MetaMask 钱包交互", "ECharts 实时图表", "ethers.js 合约调用"], color: C.blue },
    { title: "业务层", tech: "SpringBoot 2.7 + MyBatis-Plus", items: ["JWT 钱包认证", "Web3j 事件监听", "MySQL + 内存缓存"], color: C.green },
    { title: "数据层", tech: "Solidity + Hardhat + 以太坊", items: ["投票创建/投票上链", "链上唯一真相源", "事件驱动同步"], color: C.orange }
  ];
  layers.forEach((l, i) => {
    const cy = 1.25 + i * 1.4;
    slide.addShape(pres.shapes.RECTANGLE, {
      x: 0.5, y: cy, w: 9, h: 1.2,
      fill: { color: C.bgCard }, shadow: makeCardShadow()
    });
    // Left accent bar
    slide.addShape(pres.shapes.RECTANGLE, {
      x: 0.5, y: cy, w: 0.07, h: 1.2, fill: { color: l.color }
    });
    slide.addText(l.title, {
      x: 0.85, y: cy + 0.05, w: 2, h: 0.35,
      fontSize: 16, color: l.color, bold: true, margin: 0
    });
    slide.addText(l.tech, {
      x: 2.8, y: cy + 0.08, w: 3, h: 0.3,
      fontSize: 11, color: C.gray, margin: 0
    });
    slide.addText(l.items.map((t, j) => ({
      text: "• " + t,
      options: { fontSize: 12, color: C.white, breakLine: j < l.items.length - 1 }
    })), {
      x: 0.85, y: cy + 0.48, w: 3, h: 0.65,
      margin: 0
    });

    // Arrow between layers
    if (i < 2) {
      slide.addText("▼", {
        x: 4.5, y: cy + 1.2, w: 1, h: 0.2,
        fontSize: 14, color: C.darkGray, align: "center"
      });
    }
  });
}

// ============ SLIDE 4: TECH STACK ============
{
  const slide = pres.addSlide();
  slide.background = { color: C.bg };

  slide.addText("核心技术栈", {
    x: 0.8, y: 0.3, w: 8, h: 0.7,
    fontSize: 32, fontFace: "Arial Black", color: C.white, bold: true, margin: 0
  });

  const techs = [
    { cat: "智能合约", items: ["Solidity ^0.8.20", "Hardhat", "Ethers.js v6", "Chai 测试"], color: C.orange },
    { cat: "后端", items: ["SpringBoot 2.7", "MyBatis-Plus 3.5", "Web3j 4.10", "MySQL 8 + 内存缓存"], color: C.green },
    { cat: "前端", items: ["Vue 3 + Vite 5", "Element Plus", "ECharts 5", "Pinia 状态管理"], color: C.blue },
    { cat: "工具链", items: ["MetaMask", "Git", "npm / Maven", "Vercel 部署"], color: C.purple }
  ];

  techs.forEach((t, i) => {
    const col = i % 2; const row = Math.floor(i / 2);
    const cx = 0.5 + col * 4.7; const cy = 1.3 + row * 2.0;

    slide.addShape(pres.shapes.RECTANGLE, {
      x: cx, y: cy, w: 4.4, h: 1.7,
      fill: { color: C.bgCard }, shadow: makeCardShadow()
    });
    // Colored top stripe
    slide.addShape(pres.shapes.RECTANGLE, {
      x: cx, y: cy, w: 4.4, h: 0.06, fill: { color: t.color }
    });
    slide.addText(t.cat, {
      x: cx + 0.25, y: cy + 0.2, w: 4, h: 0.35,
      fontSize: 16, color: t.color, bold: true, margin: 0
    });
    slide.addText(t.items.map((item, j) => ({
      text: item,
      options: { fontSize: 12, color: C.white, breakLine: j < t.items.length - 1 }
    })), {
      x: cx + 0.25, y: cy + 0.65, w: 3.9, h: 0.9,
      margin: 0
    });
  });
}

// ============ SLIDE 5: SMART CONTRACT ============
{
  const slide = pres.addSlide();
  slide.background = { color: C.bg };

  slide.addText("智能合约设计", {
    x: 0.8, y: 0.3, w: 8, h: 0.7,
    fontSize: 32, fontFace: "Arial Black", color: C.white, bold: true, margin: 0
  });

  // Left: Contract structure
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 1.2, w: 4.5, h: 3.8,
    fill: { color: C.bgCard }, shadow: makeCardShadow()
  });
  slide.addText("VotingContract.sol", {
    x: 0.75, y: 1.3, w: 4, h: 0.35,
    fontSize: 15, color: C.orange, bold: true, margin: 0
  });
  slide.addText([
    { text: "数据结构", options: { bold: true, color: C.white, fontSize: 13, breakLine: true } },
    { text: "  Poll { id, creator, title, options[], time }", options: { color: C.gray, fontSize: 11, breakLine: true } },
    { text: "  mapping(hasVoted, voteCounts)", options: { color: C.gray, fontSize: 11, breakLine: true } },
    { text: "", options: { breakLine: true, fontSize: 6 } },
    { text: "核心函数", options: { bold: true, color: C.white, fontSize: 13, breakLine: true } },
    { text: "  createPoll(title, desc, opts, dur) → id", options: { color: C.gray, fontSize: 11, breakLine: true } },
    { text: "  vote(pollId, optionIndex)", options: { color: C.gray, fontSize: 11, breakLine: true } },
    { text: "  getPollInfo / getVoteCounts (view)", options: { color: C.gray, fontSize: 11, breakLine: true } },
    { text: "  getHasVoted / isPollActive (view)", options: { color: C.gray, fontSize: 11, breakLine: true } },
    { text: "", options: { breakLine: true, fontSize: 6 } },
    { text: "事件", options: { bold: true, color: C.white, fontSize: 13, breakLine: true } },
    { text: "  PollCreated(pollId, creator, endTime)", options: { color: C.gray, fontSize: 11, breakLine: true } },
    { text: "  VoteCasted(pollId, voter, optionIndex)", options: { color: C.gray, fontSize: 11 } }
  ], { x: 0.75, y: 1.7, w: 4, h: 3.2, valign: "top", margin: 0 });

  // Right: Design principles
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 5.3, y: 1.2, w: 4.2, h: 3.8,
    fill: { color: C.bgCard }, shadow: makeCardShadow()
  });
  slide.addText("设计原则", {
    x: 5.55, y: 1.3, w: 3.7, h: 0.35,
    fontSize: 15, color: C.blue, bold: true, margin: 0
  });
  const principles = [
    { icon: "🔒", text: "不可篡改 — 无 update/delete 函数" },
    { icon: "👁️", text: "公开可验证 — 所有数据链上存储" },
    { icon: "🛡️", text: "防重复投票 — mapping 标记状态" },
    { icon: "⏱️", text: "时间约束 — 投票期内有效" },
    { icon: "📡", text: "事件驱动 — emit 通知链下同步" },
    { icon: "✅", text: "10 个测试用例全覆盖" }
  ];
  principles.forEach((p, i) => {
    slide.addText(p.icon + "  " + p.text, {
      x: 5.55, y: 1.8 + i * 0.5, w: 3.7, h: 0.4,
      fontSize: 12, color: C.white, margin: 0
    });
  });
}

// ============ SLIDE 6: BACKEND ============
{
  const slide = pres.addSlide();
  slide.background = { color: C.bg };

  slide.addText("后端架构", {
    x: 0.8, y: 0.3, w: 8, h: 0.7,
    fontSize: 32, fontFace: "Arial Black", color: C.white, bold: true, margin: 0
  });

  // API endpoints table
  const apiData = [
    [
      { text: "方法", options: { bold: true, color: C.white, fill: { color: C.border }, fontSize: 11 } },
      { text: "路径", options: { bold: true, color: C.white, fill: { color: C.border }, fontSize: 11 } },
      { text: "说明", options: { bold: true, color: C.white, fill: { color: C.border }, fontSize: 11 } }
    ],
    ["POST", "/api/auth/nonce", "获取 MetaMask 登录随机数"],
    ["POST", "/api/auth/login", "验证签名，返回 JWT"],
    ["GET", "/api/polls", "投票列表 (MySQL 分页)"],
    ["GET", "/api/polls/{id}", "投票详情 + 实时票数"],
    ["POST", "/api/polls/sync", "上链后同步到数据库"],
    ["GET", "/api/polls/{id}/verify", "链上 vs 数据库 比对验证"]
  ].map((row, ri) => row.map((cell, ci) => ({
    text: typeof cell === "string" ? cell : cell.text,
    options: typeof cell === "string"
      ? { fontSize: 10, color: ci === 0 ? C.blue : C.white, fill: { color: ri % 2 === 0 ? C.bgCard : C.bg } }
      : { ...cell.options, fill: { color: ri % 2 === 0 ? C.bgCard : C.bg } }
  })));

  slide.addTable(apiData, {
    x: 0.5, y: 1.2, w: 5.5, colW: [0.9, 1.8, 2.8],
    border: { pt: 0.5, color: C.border },
    rowH: [0.35, 0.32, 0.32, 0.32, 0.32, 0.32, 0.32]
  });

  // Right side: event listener
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 6.3, y: 1.2, w: 3.2, h: 3.8,
    fill: { color: C.bgCard }, shadow: makeCardShadow()
  });
  slide.addText("事件监听同步", {
    x: 6.5, y: 1.3, w: 2.8, h: 0.35,
    fontSize: 15, color: C.green, bold: true, margin: 0
  });
  slide.addText([
    { text: "Web3j ethLogFlowable", options: { bold: true, color: C.white, fontSize: 11, breakLine: true } },
    { text: "", options: { breakLine: true, fontSize: 4 } },
    { text: "PollCreated → MySQL insert", options: { color: C.gray, fontSize: 11, breakLine: true } },
    { text: "VoteCasted → 缓存 + 数据库更新", options: { color: C.gray, fontSize: 11, breakLine: true } },
    { text: "", options: { breakLine: true, fontSize: 4 } },
    { text: "• EARLIEST → LATEST 全量监听", options: { color: C.gray, fontSize: 10, breakLine: true } },
    { text: "• 异步单线程处理，不阻塞启动", options: { color: C.gray, fontSize: 10, breakLine: true } },
    { text: "", options: { breakLine: true, fontSize: 4 } },
    { text: "缓存策略", options: { bold: true, color: C.white, fontSize: 11, breakLine: true } },
    { text: "", options: { breakLine: true, fontSize: 4 } },
    { text: "poll:{id}:vote_counts (Hash)", options: { color: C.blue, fontSize: 10, breakLine: true } },
    { text: "poll:{id}:voters (Set)", options: { color: C.blue, fontSize: 10, breakLine: true } },
    { text: "user:nonce:{addr} (String + TTL)", options: { color: C.blue, fontSize: 10 } }
  ], { x: 6.5, y: 1.75, w: 2.8, h: 3.1, valign: "top", margin: 0 });
}

// ============ SLIDE 7: FRONTEND ============
{
  const slide = pres.addSlide();
  slide.background = { color: C.bg };

  slide.addText("前端设计", {
    x: 0.8, y: 0.3, w: 8, h: 0.7,
    fontSize: 32, fontFace: "Arial Black", color: C.white, bold: true, margin: 0
  });

  // Pages as cards
  const pages = [
    { name: "投票列表", route: "/", desc: "首页展示所有投票卡片，分页加载" },
    { name: "投票详情", route: "/poll/:id", desc: "ECharts 柱状图 + 实时票数 + 投票操作" },
    { name: "创建投票", route: "/create", desc: "填写表单 → MetaMask 签名 → 上链" },
    { name: "链上验证", route: "/verify", desc: "输入 ID 对比链上与数据库数据" },
    { name: "个人中心", route: "/profile", desc: "JWT 认证，钱包地址展示" }
  ];

  pages.forEach((p, i) => {
    const cy = 1.2 + i * 0.8;
    slide.addShape(pres.shapes.RECTANGLE, {
      x: 0.5, y: cy, w: 5.5, h: 0.65,
      fill: { color: C.bgCard }, shadow: makeCardShadow()
    });
    // Route badge
    slide.addShape(pres.shapes.ROUNDED_RECTANGLE, {
      x: 0.7, y: cy + 0.14, w: 1.4, h: 0.36,
      fill: { color: C.bg }, rectRadius: 0.05
    });
    slide.addText(p.route, {
      x: 0.7, y: cy + 0.14, w: 1.4, h: 0.36,
      fontSize: 10, color: C.blue, align: "center", margin: 0
    });
    slide.addText(p.name, {
      x: 2.3, y: cy + 0.08, w: 1.5, h: 0.3,
      fontSize: 14, color: C.white, bold: true, margin: 0
    });
    slide.addText(p.desc, {
      x: 2.3, y: cy + 0.35, w: 3.5, h: 0.25,
      fontSize: 10, color: C.gray, margin: 0
    });
  });

  // Right: interaction flow
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 6.3, y: 1.2, w: 3.2, h: 3.7,
    fill: { color: C.bgCard }, shadow: makeCardShadow()
  });
  slide.addText("钱包交互流程", {
    x: 6.5, y: 1.3, w: 2.8, h: 0.35,
    fontSize: 15, color: C.orange, bold: true, margin: 0
  });
  const flow = ["① MetaMask 连接钱包", "② 获取 nonce 随机数", "③ personal_sign 签名", "④ 后端验证 → JWT", "⑤ ethers.js 调用合约", "⑥ tx.wait() 确认上链", "⑦ 通知后端同步数据"];
  flow.forEach((f, i) => {
    slide.addText(f, {
      x: 6.5, y: 1.8 + i * 0.4, w: 2.8, h: 0.3,
      fontSize: 11, color: i < 4 ? C.blue : C.green, margin: 0
    });
    if (i < flow.length - 1) {
      slide.addText("│", {
        x: 7.1, y: 2.08 + i * 0.4, w: 0.3, h: 0.15,
        fontSize: 10, color: C.darkGray, align: "center"
      });
    }
  });
}

// ============ SLIDE 8: DATA FLOW ============
{
  const slide = pres.addSlide();
  slide.background = { color: C.bg };

  slide.addText("混合模式数据流", {
    x: 0.8, y: 0.3, w: 8, h: 0.7,
    fontSize: 32, fontFace: "Arial Black", color: C.white, bold: true, margin: 0
  });

  // Write path
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 1.2, w: 4.3, h: 3.6,
    fill: { color: C.bgCard }, shadow: makeCardShadow()
  });
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 1.2, w: 4.3, h: 0.06, fill: { color: C.orange }
  });
  slide.addText("✏️ 写操作 (createPoll / vote)", {
    x: 0.75, y: 1.35, w: 3.8, h: 0.35,
    fontSize: 14, color: C.orange, bold: true, margin: 0
  });
  const writeFlow = [
    "1. 用户在浏览器填写表单 / 选择选项",
    "2. ethers.js 构建合约调用",
    "3. MetaMask 弹出签名确认窗口",
    "4. 用户签名 → 广播交易到以太坊网络",
    "5. 矿工打包 → 返回 txHash",
    "6. 前端拿 txHash 调 POST /sync",
    "7. 后端从链上读取数据 → MySQL + 缓存"
  ];
  writeFlow.forEach((f, i) => {
    slide.addText(f, {
      x: 0.75, y: 1.85 + i * 0.38, w: 3.8, h: 0.3,
      fontSize: 10, color: C.white, margin: 0
    });
  });

  // Read path
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 5.2, y: 1.2, w: 4.3, h: 3.6,
    fill: { color: C.bgCard }, shadow: makeCardShadow()
  });
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 5.2, y: 1.2, w: 4.3, h: 0.06, fill: { color: C.blue }
  });
  slide.addText("📖 读操作 (列表 / 详情)", {
    x: 5.45, y: 1.35, w: 3.8, h: 0.35,
    fontSize: 14, color: C.blue, bold: true, margin: 0
  });
  const readFlow = [
    "1. 前端 axios → SpringBoot API",
    "2. 后端先查缓存 (内存)",
    "3. 缓存 miss → 查 MySQL",
    "4. 回写缓存 + 返回 JSON",
    "5. 前端 ECharts 渲染图表",
    "",
    "用户也可直接 ethers.js 读合约",
    "→ 用于「链上验证」功能"
  ];
  readFlow.forEach((f, i) => {
    slide.addText(f, {
      x: 5.45, y: 1.85 + i * 0.38, w: 3.8, h: 0.3,
      fontSize: 10, color: C.white, margin: 0
    });
  });
}

// ============ SLIDE 9: LOCAL DEPLOYMENT ============
{
  const slide = pres.addSlide();
  slide.background = { color: C.bg };

  slide.addText("本地部署方案", {
    x: 0.8, y: 0.3, w: 8, h: 0.7,
    fontSize: 32, fontFace: "Arial Black", color: C.white, bold: true, margin: 0
  });

  // Steps
  const steps = [
    { num: "1", title: "启动 Hardhat 节点", cmd: "cd contracts && npx hardhat node", port: "localhost:8545", color: C.orange },
    { num: "2", title: "部署智能合约", cmd: "npx hardhat run scripts/deploy.js --network localhost", port: "→ 获取合约地址", color: C.orange },
    { num: "3", title: "初始化数据库", cmd: "执行 init.sql → 建表 polls / users / poll_results", port: "MySQL 3306", color: C.green },
    { num: "4", title: "启动后端", cmd: "mvn spring-boot:run", port: "localhost:8080", color: C.green },
    { num: "5", title: "启动前端", cmd: "npm run dev", port: "localhost:5173", color: C.blue }
  ];

  steps.forEach((s, i) => {
    const sy = 1.2 + i * 0.78;
    slide.addShape(pres.shapes.RECTANGLE, {
      x: 0.5, y: sy, w: 9, h: 0.65,
      fill: { color: C.bgCard }, shadow: makeCardShadow()
    });
    // Number circle
    slide.addShape(pres.shapes.OVAL, {
      x: 0.7, y: sy + 0.12, w: 0.4, h: 0.4,
      fill: { color: s.color }
    });
    slide.addText(s.num, {
      x: 0.7, y: sy + 0.12, w: 0.4, h: 0.4,
      fontSize: 16, color: C.bg, bold: true, align: "center", valign: "middle"
    });
    slide.addText(s.title, {
      x: 1.3, y: sy + 0.03, w: 2.5, h: 0.3,
      fontSize: 14, color: C.white, bold: true, margin: 0
    });
    slide.addText(s.cmd, {
      x: 1.3, y: sy + 0.33, w: 5.5, h: 0.25,
      fontSize: 10, color: C.gray, margin: 0
    });
    slide.addText(s.port, {
      x: 7.5, y: sy + 0.15, w: 1.8, h: 0.35,
      fontSize: 10, color: s.color, align: "right", margin: 0
    });
  });

  // Bottom note
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.05, w: 9, h: 0.05, fill: { color: C.border }
  });
  slide.addText("⚠ 所有服务均在本地运行，Hardhat 提供 20 个预存 10000 ETH 的测试账户", {
    x: 0.8, y: 5.1, w: 8.5, h: 0.3,
    fontSize: 10, color: C.gray, margin: 0
  });
}

// ============ SLIDE 10: FEATURE DEMO ============
{
  const slide = pres.addSlide();
  slide.background = { color: C.bg };

  slide.addText("功能演示要点", {
    x: 0.8, y: 0.3, w: 8, h: 0.7,
    fontSize: 32, fontFace: "Arial Black", color: C.white, bold: true, margin: 0
  });

  const features = [
    { icon: "🔗", title: "钱包连接", desc: "MetaMask 连接 → 签名登录 → JWT 认证" },
    { icon: "📝", title: "创建投票", desc: "填写表单 → MetaMask 签名 → txHash 确认" },
    { icon: "🗳️", title: "投票", desc: "实时 ECharts 图表 → 选选项 → 链上交易" },
    { icon: "📊", title: "实时统计", desc: "投票后立即刷新图表，票数自动同步" },
    { icon: "✅", title: "链上验证", desc: "对比合约数据与数据库，确保一致性" },
    { icon: "👥", title: "多人协作", desc: "同一局域网内，多人可同时投票" }
  ];

  features.forEach((f, i) => {
    const col = i % 3; const row = Math.floor(i / 3);
    const cx = 0.5 + col * 3.1; const cy = 1.3 + row * 2.0;

    slide.addShape(pres.shapes.RECTANGLE, {
      x: cx, y: cy, w: 2.8, h: 1.7,
      fill: { color: C.bgCard }, shadow: makeCardShadow()
    });
    slide.addText(f.icon, {
      x: cx, y: cy + 0.25, w: 0.7, h: 0.7, fontSize: 28, align: "center"
    });
    slide.addText(f.title, {
      x: cx + 0.8, y: cy + 0.3, w: 1.8, h: 0.4,
      fontSize: 15, color: C.white, bold: true, margin: 0
    });
    slide.addText(f.desc, {
      x: cx + 0.25, y: cy + 0.9, w: 2.3, h: 0.6,
      fontSize: 11, color: C.gray, margin: 0
    });
  });
}

// ============ SLIDE 11: TECHNICAL HIGHLIGHTS ============
{
  const slide = pres.addSlide();
  slide.background = { color: C.bg };

  slide.addText("技术亮点", {
    x: 0.8, y: 0.3, w: 8, h: 0.7,
    fontSize: 32, fontFace: "Arial Black", color: C.white, bold: true, margin: 0
  });

  const highlights = [
    { title: "混合架构", desc: "写操作直接上链保证不可篡改，读操作走后端索引提升性能，兼顾安全与体验" },
    { title: "事件驱动同步", desc: "Web3j 监听 PollCreated / VoteCasted 事件，自动同步 MySQL + 缓存，链上为唯一真相源" },
    { title: "钱包签名认证", desc: "MetaMask personal_sign 签名 + nonce 防重放，无需密码即可安全登录" },
    { title: "EIP-712 风格签名", desc: "用户明确看到签名内容 'Login to DApp Voting: {nonce}'，防止盲签攻击" },
    { title: "链上验证机制", desc: "前端可独立通过 ethers.js 读合约，与后端数据对比，任何人可验证数据真实性" },
    { title: "模块化设计", desc: "合约 / 后端 / 前端三层独立，合约可单独测试部署，后端可替换缓存方案" }
  ];

  highlights.forEach((h, i) => {
    const col = i % 2; const row = Math.floor(i / 2);
    const cx = 0.5 + col * 4.7; const cy = 1.2 + row * 1.4;

    slide.addShape(pres.shapes.RECTANGLE, {
      x: cx, y: cy, w: 4.4, h: 1.15,
      fill: { color: C.bgCard }, shadow: makeCardShadow()
    });
    slide.addShape(pres.shapes.RECTANGLE, {
      x: cx, y: cy, w: 0.07, h: 1.15, fill: { color: C.blue }
    });
    slide.addText(h.title, {
      x: cx + 0.3, y: cy + 0.1, w: 3.9, h: 0.35,
      fontSize: 14, color: C.blue, bold: true, margin: 0
    });
    slide.addText(h.desc, {
      x: cx + 0.3, y: cy + 0.5, w: 3.9, h: 0.55,
      fontSize: 11, color: C.gray, margin: 0
    });
  });
}

// ============ SLIDE 12: SUMMARY ============
{
  const slide = pres.addSlide();
  slide.background = { color: C.bg };

  // Decorative shapes
  slide.addShape(pres.shapes.OVAL, {
    x: -1, y: 3.5, w: 3, h: 3,
    fill: { color: C.blue, transparency: 92 }
  });
  slide.addShape(pres.shapes.OVAL, {
    x: 8, y: -0.5, w: 3.5, h: 3.5,
    fill: { color: C.purple, transparency: 90 }
  });

  slide.addText("总结与展望", {
    x: 0.8, y: 0.5, w: 8, h: 0.8,
    fontSize: 36, fontFace: "Arial Black", color: C.white, bold: true, margin: 0
  });

  // Summary cards
  const summaryItems = [
    { title: "已完成", items: ["Solidity 合约 + 10 个测试用例", "SpringBoot 6 个 API 端点", "Vue3 5 个页面 + 3 个组件", "Web3j 事件监听 + 数据同步"], color: C.green },
    { title: "待优化", items: ["部署至 Sepolia 公测网", "前端部署至 Vercel", "后端部署至 Render", "Redis 替换内存缓存"], color: C.orange }
  ];

  summaryItems.forEach((s, i) => {
    const cx = 0.5 + i * 4.7;
    slide.addShape(pres.shapes.RECTANGLE, {
      x: cx, y: 1.5, w: 4.4, h: 3.0,
      fill: { color: C.bgCard }, shadow: makeCardShadow()
    });
    slide.addShape(pres.shapes.RECTANGLE, {
      x: cx, y: 1.5, w: 4.4, h: 0.05, fill: { color: s.color }
    });
    slide.addText(s.title, {
      x: cx + 0.3, y: 1.65, w: 3.8, h: 0.4,
      fontSize: 18, color: s.color, bold: true, margin: 0
    });
    slide.addText(s.items.map((item, j) => ({
      text: "✓  " + item,
      options: { fontSize: 12, color: C.white, breakLine: j < s.items.length - 1 }
    })), {
      x: cx + 0.3, y: 2.2, w: 3.8, h: 2, valign: "top", margin: 0
    });
  });

  // Thank you
  slide.addText("谢谢！欢迎提问", {
    x: 1, y: 4.8, w: 8, h: 0.6,
    fontSize: 22, color: C.blue, italic: true, align: "center", margin: 0
  });
}

// ============ GENERATE ============
const outputPath = "D:/项目管理/project-select/docs/去中心化投票系统-答辩PPT.pptx";
pres.writeFile({ fileName: outputPath })
  .then(() => console.log("PPT 已生成: " + outputPath))
  .catch(err => console.error("生成失败:", err));
