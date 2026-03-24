package com.example.stockanalysis.utils

/**
 * 拼音搜索辅助类
 * 支持拼音首字母、全拼、别名搜索
 */
object PinyinSearchHelper {
    
    /**
     * 常用汉字到拼音首字母的映射表
     * 用于简单的拼音首字母搜索
     */
    private val pinyinFirstLetters = mapOf(
        // A
        '阿' to 'a', '艾' to 'a', '安' to 'a', '奥' to 'a', '澳' to 'a',
        // B
        '巴' to 'b', '白' to 'b', '百' to 'b', '拜' to 'b', '班' to 'b',
        '宝' to 'b', '保' to 'b', '北' to 'b', '贝' to 'b', '本' to 'b',
        '比' to 'b', '币' to 'b', '必' to 'b', '毕' to 'b', '边' to 'b',
        '变' to 'b', '标' to 'b', '表' to 'b', '别' to 'b', '宾' to 'b',
        '兵' to 'b', '冰' to 'b', '波' to 'b', '博' to 'b', '勃' to 'b',
        '泊' to 'b', '博' to 'b', '不' to 'b', '布' to 'b', '步' to 'b',
        '部' to 'b',
        // C
        '才' to 'c', '材' to 'c', '财' to 'c', '采' to 'c', '彩' to 'c',
        '参' to 'c', '餐' to 'c', '残' to 'c', '蚕' to 'c', '仓' to 'c',
        '沧' to 'c', '藏' to 'c', '操' to 'c', '曹' to 'c', '草' to 'c',
        '册' to 'c', '侧' to 'c', '测' to 'c', '策' to 'c', '层' to 'c',
        '查' to 'c', '茶' to 'c', '察' to 'c', '差' to 'c', '产' to 'c',
        '长' to 'c', '常' to 'c', '偿' to 'c', '厂' to 'c', '场' to 'c',
        '超' to 'c', '潮' to 'c', '车' to 'c', '扯' to 'c', '彻' to 'c',
        '沉' to 'c', '陈' to 'c', '晨' to 'c', '称' to 'c', '城' to 'c',
        '成' to 'c', '承' to 'c', '诚' to 'c', '吃' to 'c', '持' to 'c',
        '池' to 'c', '迟' to 'c', '驰' to 'c', '尺' to 'c', '冲' to 'c',
        '充' to 'c', '重' to 'c', '崇' to 'c', '抽' to 'c', '仇' to 'c',
        '筹' to 'c', '丑' to 'c', '初' to 'c', '出' to 'c', '础' to 'c',
        '储' to 'c', '处' to 'c', '川' to 'c', '穿' to 'c', '传' to 'c',
        '船' to 'c', '创' to 'c', '春' to 'c', '纯' to 'c', '词' to 'c',
        '此' to 'c', '次' to 'c', '刺' to 'c', '从' to 'c', '匆' to 'c',
        '葱' to 'c', '聪' to 'c', '粗' to 'c', '促' to 'c', '村' to 'c',
        '存' to 'c', '寸' to 'c', '错' to 'c',
        // D
        '达' to 'd', '答' to 'd', '打' to 'd', '大' to 'd', '代' to 'd',
        '带' to 'd', '待' to 'd', '袋' to 'd', '贷' to 'd', '戴' to 'd',
        '丹' to 'd', '单' to 'd', '担' to 'd', '胆' to 'd', '旦' to 'd',
        '但' to 'd', '诞' to 'd', '弹' to 'd', '当' to 'd', '挡' to 'd',
        '党' to 'd', '荡' to 'd', '刀' to 'd', '导' to 'd', '岛' to 'd',
        '倒' to 'd', '稻' to 'd', '德' to 'd', '登' to 'd', '等' to 'd',
        '邓' to 'd', '堤' to 'd', '提' to 'd', '迪' to 'd', '敌' to 'd',
        '笛' to 'd', '底' to 'd', '地' to 'd', '弟' to 'd', '帝' to 'd',
        '递' to 'd', '第' to 'd', '典' to 'd', '电' to 'd', '店' to 'd',
        '淀' to 'd', '定' to 'd', '东' to 'd', '冬' to 'd', '董' to 'd',
        '懂' to 'd', '动' to 'd', '冻' to 'd', '洞' to 'd', '都' to 'd',
        '斗' to 'd', '豆' to 'd', '督' to 'd', '毒' to 'd', '读' to 'd',
        '独' to 'd', '堵' to 'd', '赌' to 'd', '杜' to 'd', '度' to 'd',
        '渡' to 'd', '端' to 'd', '短' to 'd', '段' to 'd', '断' to 'd',
        '队' to 'd', '对' to 'd', '多' to 'd',
        // E
        '俄' to 'e', '鹅' to 'e', '额' to 'e', '恶' to 'e', '恩' to 'e',
        '儿' to 'e', '而' to 'e', '耳' to 'e', '二' to 'e',
        // F
        '发' to 'f', '乏' to 'f', '伐' to 'f', '罚' to 'f', '法' to 'f',
        '帆' to 'f', '番' to 'f', '翻' to 'f', '凡' to 'f', '烦' to 'f',
        '反' to 'f', '返' to 'f', '范' to 'f', '贩' to 'f', '方' to 'f',
        '芳' to 'f', '防' to 'f', '房' to 'f', '飞' to 'f', '非' to 'f',
        '菲' to 'f', '肥' to 'f', '废' to 'f', '费' to 'f', '分' to 'f',
        '纷' to 'f', '芬' to 'f', '坟' to 'f', '粉' to 'f', '份' to 'f',
        '丰' to 'f', '风' to 'f', '枫' to 'f', '封' to 'f', '峰' to 'f',
        '锋' to 'f', '蜂' to 'f', '冯' to 'f', '逢' to 'f', '缝' to 'f',
        '凤' to 'f', '奉' to 'f', '佛' to 'f', '否' to 'f', '夫' to 'f',
        '肤' to 'f', '扶' to 'f', '服' to 'f', '浮' to 'f', '符' to 'f',
        '福' to 'f', '抚' to 'f', '府' to 'f', '俯' to 'f', '辅' to 'f',
        '腐' to 'f', '父' to 'f', '付' to 'f', '负' to 'f', '妇' to 'f',
        '附' to 'f', '复' to 'f', '赴' to 'f', '副' to 'f', '富' to 'f',
        // G
        '改' to 'g', '概' to 'g', '干' to 'g', '甘' to 'g', '杆' to 'g',
        '肝' to 'g', '赶' to 'g', '敢' to 'g', '感' to 'g', '干' to 'g',
        '刚' to 'g', '岗' to 'g', '纲' to 'g', '缸' to 'g', '钢' to 'g',
        '港' to 'g', '高' to 'g', '搞' to 'g', '告' to 'g', '戈' to 'g',
        '哥' to 'g', '歌' to 'g', '鸽' to 'g', '割' to 'g', '革' to 'g',
        '格' to 'g', '葛' to 'g', '隔' to 'g', '个' to 'g', '各' to 'g',
        '给' to 'g', '根' to 'g', '跟' to 'g', '更' to 'g', '耕' to 'g',
        '工' to 'g', '弓' to 'g', '公' to 'g', '功' to 'g', '攻' to 'g',
        '供' to 'g', '宫' to 'g', '恭' to 'g', '巩' to 'g', '共' to 'g',
        '贡' to 'g', '勾' to 'g', '沟' to 'g', '构' to 'g', '购' to 'g',
        '够' to 'g', '估' to 'g', '孤' to 'g', '姑' to 'g', '骨' to 'g',
        '古' to 'g', '谷' to 'g', '股' to 'g', '故' to 'g', '固' to 'g',
        '顾' to 'g', '瓜' to 'g', '刮' to 'g', '寡' to 'g', '挂' to 'g',
        '乖' to 'g', '拐' to 'g', '怪' to 'g', '关' to 'g', '观' to 'g',
        '官' to 'g', '馆' to 'g', '管' to 'g', '贯' to 'g', '惯' to 'g',
        '灌' to 'g', '光' to 'g', '广' to 'g', '逛' to 'g', '归' to 'g',
        '龟' to 'g', '规' to 'g', '轨' to 'g', '鬼' to 'g', '贵' to 'g',
        '桂' to 'g', '滚' to 'g', '国' to 'g', '果' to 'g', '过' to 'g',
        // H
        '哈' to 'h', '海' to 'h', '害' to 'h', '含' to 'h', '寒' to 'h',
        '韩' to 'h', '罕' to 'h', '喊' to 'h', '汉' to 'h', '杭' to 'h',
        '航' to 'h', '毫' to 'h', '豪' to 'h', '好' to 'h', '号' to 'h',
        '浩' to 'h', '耗' to 'h', '呵' to 'h', '喝' to 'h', '合' to 'h',
        '何' to 'h', '和' to 'h', '河' to 'h', '核' to 'h', '荷' to 'h',
        '贺' to 'h', '赫' to 'h', '褐' to 'h', '鹤' to 'h', '黑' to 'h',
        '痕' to 'h', '很' to 'h', '狠' to 'h', '恨' to 'h', '恒' to 'h',
        '横' to 'h', '衡' to 'h', '轰' to 'h', '虹' to 'h', '宏' to 'h',
        '洪' to 'h', '红' to 'h', '侯' to 'h', '厚' to 'h', '后' to 'h',
        '乎' to 'h', '呼' to 'h', '忽' to 'h', '狐' to 'h', '胡' to 'h',
        '湖' to 'h', '葫' to 'h', '糊' to 'h', '蝴' to 'h', '虎' to 'h',
        '互' to 'h', '户' to 'h', '护' to 'h', '花' to 'h', '华' to 'h',
        '化' to 'h', '划' to 'h', '画' to 'h', '话' to 'h', '怀' to 'h',
        '淮' to 'h', '坏' to 'h', '欢' to 'h', '还' to 'h', '环' to 'h',
        '缓' to 'h', '换' to 'h', '患' to 'h', '荒' to 'h', '皇' to 'h',
        '黄' to 'h', '煌' to 'h', '晃' to 'h', '灰' to 'h', '恢' to 'h',
        '挥' to 'h', '回' to 'h', '汇' to 'h', '会' to 'h', '绘' to 'h',
        '惠' to 'h', '慧' to 'h', '昏' to 'h', '婚' to 'h', '浑' to 'h',
        '魂' to 'h', '混' to 'h', '活' to 'h', '火' to 'h', '获' to 'h',
        '或' to 'h', '货' to 'h', '祸' to 'h', '惑' to 'h', '霍' to 'h',
        // J
        '机' to 'j', '击' to 'j', '积' to 'j', '基' to 'j', '激' to 'j',
        '及' to 'j', '吉' to 'j', '级' to 'j', '即' to 'j', '极' to 'j',
        '集' to 'j', '急' to 'j', '疾' to 'j', '籍' to 'j', '集' to 'j',
        '几' to 'j', '己' to 'j', '计' to 'j', '记' to 'j', '技' to 'j',
        '际' to 'j', '剂' to 'j', '济' to 'j', '继' to 'j', '寄' to 'j',
        '加' to 'j', '夹' to 'j', '佳' to 'j', '家' to 'j', '嘉' to 'j',
        '甲' to 'j', '价' to 'j', '驾' to 'j', '架' to 'j', '监' to 'j',
        '坚' to 'j', '间' to 'j', '艰' to 'j', '兼' to 'j', '肩' to 'j',
        '简' to 'j', '见' to 'j', '件' to 'j', '建' to 'j', '剑' to 'j',
        '荐' to 'j', '贱' to 'j', '健' to 'j', '舰' to 'j', '渐' to 'j',
        '践' to 'j', '鉴' to 'j', '键' to 'j', '江' to 'j', '将' to 'j',
        '姜' to 'j', '浆' to 'j', '僵' to 'j', '讲' to 'j', '奖' to 'j',
        '匠' to 'j', '降' to 'j', '交' to 'j', '郊' to 'j', '娇' to 'j',
        '角' to 'j', '饺' to 'j', '脚' to 'j', '搅' to 'j', '叫' to 'j',
        '较' to 'j', '教' to 'j', '阶' to 'j', '皆' to 'j', '接' to 'j',
        '揭' to 'j', '街' to 'j', '节' to 'j', '劫' to 'j', '杰' to 'j',
        '洁' to 'j', '结' to 'j', '捷' to 'j', '截' to 'j', '姐' to 'j',
        '解' to 'j', '介' to 'j', '戒' to 'j', '界' to 'j', '金' to 'j',
        '今' to 'j', '仅' to 'j', '紧' to 'j', '锦' to 'j', '尽' to 'j',
        '近' to 'j', '进' to 'j', '晋' to 'j', '浸' to 'j', '京' to 'j',
        '经' to 'j', '惊' to 'j', '晶' to 'j', '精' to 'j', '鲸' to 'j',
        '井' to 'j', '景' to 'j', '警' to 'j', '净' to 'j', '竞' to 'j',
        '竟' to 'j', '敬' to 'j', '境' to 'j', '静' to 'j', '镜' to 'j',
        '九' to 'j', '久' to 'j', '酒' to 'j', '旧' to 'j', '救' to 'j',
        '就' to 'j', '居' to 'j', '局' to 'j', '举' to 'j', '矩' to 'j',
        '句' to 'j', '巨' to 'j', '拒' to 'j', '具' to 'j', '俱' to 'j',
        '剧' to 'j', '惧' to 'j', '据' to 'j', '距' to 'j', '聚' to 'j',
        '卷' to 'j', '决' to 'j', '绝' to 'j', '觉' to 'j', '军' to 'j',
        '君' to 'j', '均' to 'j', '菌' to 'j', '俊' to 'j',
        // K
        '咖' to 'k', '卡' to 'k', '开' to 'k', '凯' to 'k', '慨' to 'k',
        '刊' to 'k', '堪' to 'k', '砍' to 'k', '看' to 'k', '康' to 'k',
        '扛' to 'k', '抗' to 'k', '炕' to 'k', '考' to 'k', '靠' to 'k',
        '科' to 'k', '壳' to 'k', '咳' to 'k', '可' to 'k', '克' to 'k',
        '刻' to 'k', '客' to 'k', '课' to 'k', '肯' to 'k', '坑' to 'k',
        '空' to 'k', '孔' to 'k', '恐' to 'k', '控' to 'k', '口' to 'k',
        '扣' to 'k', '苦' to 'k', '库' to 'k', '裤' to 'k', '夸' to 'k',
        '垮' to 'k', '跨' to 'k', '块' to 'k', '快' to 'k', '宽' to 'k',
        '款' to 'k', '狂' to 'k', '况' to 'k', '矿' to 'k', '亏' to 'k',
        '葵' to 'k', '愧' to 'k', '坤' to 'k', '困' to 'k', '扩' to 'k',
        // L
        '拉' to 'l', '啦' to 'l', '落' to 'l', '腊' to 'l', '辣' to 'l',
        '来' to 'l', '赖' to 'l', '兰' to 'l', '栏' to 'l', '蓝' to 'l',
        '览' to 'l', '懒' to 'l', '烂' to 'l', '狼' to 'l', '朗' to 'l',
        '浪' to 'l', '捞' to 'l', '劳' to 'l', '老' to 'l', '乐' to 'l',
        '勒' to 'l', '雷' to 'l', '累' to 'l', '冷' to 'l', '愣' to 'l',
        '黎' to 'l', '礼' to 'l', '李' to 'l', '里' to 'l', '理' to 'l',
        '力' to 'l', '历' to 'l', '立' to 'l', '丽' to 'l', '利' to 'l',
        '励' to 'l', '例' to 'l', '隶' to 'l', '连' to 'l', '帘' to 'l',
        '怜' to 'l', '莲' to 'l', '联' to 'l', '廉' to 'l', '练' to 'l',
        '炼' to 'l', '恋' to 'l', '良' to 'l', '凉' to 'l', '梁' to 'l',
        '粮' to 'l', '两' to 'l', '亮' to 'l', '谅' to 'l', '辽' to 'l',
        '疗' to 'l', '聊' to 'l', '僚' to 'l', '廖' to 'l', '料' to 'l',
        '列' to 'l', '劣' to 'l', '林' to 'l', '临' to 'l', '淋' to 'l',
        '零' to 'l', '龄' to 'l', '领' to 'l', '令' to 'l', '溜' to 'l',
        '刘' to 'l', '流' to 'l', '留' to 'l', '龙' to 'l', '隆' to 'l',
        '垄' to 'l', '拢' to 'l', '楼' to 'l', '漏' to 'l', '露' to 'l',
        '卢' to 'l', '鲁' to 'l', '陆' to 'l', '录' to 'l', '鹿' to 'l',
        '滤' to 'l', '卵' to 'l', '乱' to 'l', '掠' to 'l', '略' to 'l',
        '伦' to 'l', '轮' to 'l', '论' to 'l', '罗' to 'l', '逻' to 'l',
        '落' to 'l', '络' to 'l', '骆' to 'l',
        // M
        '妈' to 'm', '麻' to 'm', '马' to 'm', '骂' to 'm', '吗' to 'm',
        '埋' to 'm', '买' to 'm', '迈' to 'm', '麦' to 'm', '卖' to 'm',
        '脉' to 'm', '蛮' to 'm', '满' to 'm', '曼' to 'm', '慢' to 'm',
        '漫' to 'm', '忙' to 'm', '芒' to 'm', '盲' to 'm', '莽' to 'm',
        '猫' to 'm', '毛' to 'm', '矛' to 'm', '茅' to 'm', '茂' to 'm',
        '贸' to 'm', '帽' to 'm', '貌' to 'm', '么' to 'm', '没' to 'm',
        '梅' to 'm', '媒' to 'm', '煤' to 'm', '每' to 'm', '美' to 'm',
        '妹' to 'm', '门' to 'm', '闷' to 'm', '盟' to 'm', '猛' to 'm',
        '梦' to 'm', '弥' to 'm', '迷' to 'm', '米' to 'm', '秘' to 'm',
        '密' to 'm', '蜜' to 'm', '眠' to 'm', '棉' to 'm', '免' to 'm',
        '勉' to 'm', '面' to 'm', '苗' to 'm', '描' to 'm', '秒' to 'm',
        '妙' to 'm', '庙' to 'm', '灭' to 'm', '民' to 'm', '敏' to 'm',
        '名' to 'm', '明' to 'm', '命' to 'm', '摸' to 'm', '模' to 'm',
        '膜' to 'm', '摩' to 'm', '磨' to 'm', '抹' to 'm', '末' to 'm',
        '沫' to 'm', '莫' to 'm', '漠' to 'm', '默' to 'm', '谋' to 'm',
        '某' to 'm', '母' to 'm', '亩' to 'm', '木' to 'm', '目' to 'm',
        '牧' to 'm', '墓' to 'm', '幕' to 'm', '慕' to 'm',
        // N
        '拿' to 'n', '哪' to 'n', '内' to 'n', '那' to 'n', '纳' to 'n',
        '乃' to 'n', '奶' to 'n', '奈' to 'n', '耐' to 'n', '男' to 'n',
        '南' to 'n', '难' to 'n', '囊' to 'n', '挠' to 'n', '恼' to 'n',
        '脑' to 'n', '闹' to 'n', '能' to 'n', '尼' to 'n', '泥' to 'n',
        '你' to 'n', '拟' to 'n', '逆' to 'n', '年' to 'n', '念' to 'n',
        '娘' to 'n', '酿' to 'n', '鸟' to 'n', '尿' to 'n', '宁' to 'n',
        '凝' to 'n', '牛' to 'n', '扭' to 'n', '浓' to 'n', '农' to 'n',
        '弄' to 'n', '奴' to 'n', '努' to 'n', '女' to 'n', '暖' to 'n',
        // O
        '欧' to 'o', '偶' to 'o',
        // P
        '爬' to 'p', '怕' to 'p', '拍' to 'p', '排' to 'p', '派' to 'p',
        '攀' to 'p', '盘' to 'p', '判' to 'p', '盼' to 'p', '旁' to 'p',
        '胖' to 'p', '抛' to 'p', '刨' to 'p', '炮' to 'p', '袍' to 'p',
        '跑' to 'p', '泡' to 'p', '陪' to 'p', '培' to 'p', '赔' to 'p',
        '佩' to 'p', '配' to 'p', '喷' to 'p', '盆' to 'p', '朋' to 'p',
        '捧' to 'p', '碰' to 'p', '批' to 'p', '披' to 'p', '皮' to 'p',
        '疲' to 'p', '脾' to 'p', '匹' to 'p', '屁' to 'p', '僻' to 'p',
        '片' to 'p', '偏' to 'p', '骗' to 'p', '漂' to 'p', '飘' to 'p',
        '票' to 'p', '撇' to 'p', '拼' to 'p', '贫' to 'p', '品' to 'p',
        '平' to 'p', '评' to 'p', '凭' to 'p', '苹' to 'p', '屏' to 'p',
        '瓶' to 'p', '坡' to 'p', '泼' to 'p', '颇' to 'p', '婆' to 'p',
        '迫' to 'p', '破' to 'p', '魄' to 'p', '剖' to 'p', '仆' to 'p',
        '扑' to 'p', '铺' to 'p', '葡' to 'p', '蒲' to 'p', '朴' to 'p',
        '圃' to 'p', '浦' to 'p', '普' to 'p', '谱' to 'p',
        // Q
        '七' to 'q', '妻' to 'q', '戚' to 'q', '期' to 'q', '欺' to 'q',
        '漆' to 'q', '齐' to 'q', '其' to 'q', '奇' to 'q', '歧' to 'q',
        '骑' to 'q', '棋' to 'q', '旗' to 'q', '企' to 'q', '岂' to 'q',
        '启' to 'q', '起' to 'q', '气' to 'q', '讫' to 'q', '迄' to 'q',
        '汽' to 'q', '泣' to 'q', '契' to 'q', '砌' to 'q', '器' to 'q',
        '恰' to 'q', '洽' to 'q', '千' to 'q', '迁' to 'q', '牵' to 'q',
        '铅' to 'q', '谦' to 'q', '签' to 'q', '前' to 'q', '钱' to 'q',
        '乾' to 'q', '潜' to 'q', '浅' to 'q', '遣' to 'q', '谴' to 'q',
        '欠' to 'q', '枪' to 'q', '腔' to 'q', '强' to 'q', '墙' to 'q',
        '抢' to 'q', '悄' to 'q', '敲' to 'q', '锹' to 'q', '乔' to 'q',
        '侨' to 'q', '桥' to 'q', '瞧' to 'q', '巧' to 'q', '俏' to 'q',
        '切' to 'q', '茄' to 'q', '且' to 'q', '窃' to 'q', '亲' to 'q',
        '侵' to 'q', '钦' to 'q', '琴' to 'q', '禽' to 'q', '勤' to 'q',
        '擒' to 'q', '寝' to 'q', '沁' to 'q', '青' to 'q', '轻' to 'q',
        '氢' to 'q', '倾' to 'q', '卿' to 'q', '清' to 'q', '蜻' to 'q',
        '情' to 'q', '晴' to 'q', '擎' to 'q', '顷' to 'q', '请' to 'q',
        '庆' to 'q', '穷' to 'q', '琼' to 'q', '丘' to 'q', '秋' to 'q',
        '蚯' to 'q', '求' to 'q', '球' to 'q', '区' to 'q', '曲' to 'q',
        '驱' to 'q', '屈' to 'q', '躯' to 'q', '趋' to 'q', '渠' to 'q',
        '取' to 'q', '娶' to 'q', '去' to 'q', '趣' to 'q',
        // R
        '然' to 'r', '燃' to 'r', '染' to 'r', '嚷' to 'r', '壤' to 'r',
        '让' to 'r', '饶' to 'r', '扰' to 'r', '绕' to 'r', '惹' to 'r',
        '热' to 'r', '人' to 'r', '仁' to 'r', '忍' to 'r', '认' to 'r',
        '任' to 'r', '刃' to 'r', '纫' to 'r', '扔' to 'r', '仍' to 'r',
        '日' to 'r', '绒' to 'r', '荣' to 'r', '容' to 'r', '溶' to 'r',
        '熔' to 'r', '融' to 'r', '冗' to 'r', '柔' to 'r', '肉' to 'r',
        '如' to 'r', '茹' to 'r', '儒' to 'r', '孺' to 'r', '辱' to 'r',
        '入' to 'r', '软' to 'r', '锐' to 'r', '瑞' to 'r', '润' to 'r',
        // S
        '三' to 's', '伞' to 's', '散' to 's', '桑' to 's', '嗓' to 's',
        '丧' to 's', '搔' to 's', '骚' to 's', '扫' to 's', '嫂' to 's',
        '色' to 's', '涩' to 's', '森' to 's', '僧' to 's', '杀' to 's',
        '沙' to 's', '纱' to 's', '刹' to 's', '砂' to 's', '傻' to 's',
        '煞' to 's', '筛' to 's', '晒' to 's', '山' to 's', '杉' to 's',
        '衫' to 's', '闪' to 's', '陕' to 's', '扇' to 's', '善' to 's',
        '伤' to 's', '商' to 's', '裳' to 's', '赏' to 's', '上' to 's',
        '尚' to 's', '捎' to 's', '梢' to 's', '烧' to 's', '稍' to 's',
        '勺' to 's', '少' to 's', '绍' to 's', '哨' to 's', '奢' to 's',
        '舌' to 's', '蛇' to 's', '舍' to 's', '设' to 's', '社' to 's',
        '射' to 's', '涉' to 's', '摄' to 's', '申' to 's', '伸' to 's',
        '身' to 's', '深' to 's', '神' to 's', '审' to 's', '婶' to 's',
        '肾' to 's', '甚' to 's', '渗' to 's', '慎' to 's', '升' to 's',
        '生' to 's', '声' to 's', '牲' to 's', '胜' to 's', '绳' to 's',
        '省' to 's', '圣' to 's', '剩' to 's', '尸' to 's', '失' to 's',
        '师' to 's', '诗' to 's', '狮' to 's', '施' to 's', '湿' to 's',
        '十' to 's', '什' to 's', '石' to 's', '时' to 's', '识' to 's',
        '实' to 's', '拾' to 's', '食' to 's', '蚀' to 's', '史' to 's',
        '使' to 's', '始' to 's', '驶' to 's', '士' to 's', '氏' to 's',
        '世' to 's', '市' to 's', '示' to 's', '式' to 's', '事' to 's',
        '侍' to 's', '势' to 's', '视' to 's', '试' to 's', '饰' to 's',
        '室' to 's', '是' to 's', '适' to 's', '逝' to 's', '释' to 's',
        '誓' to 's', '收' to 's', '手' to 's', '守' to 's', '首' to 's',
        '寿' to 's', '受' to 's', '兽' to 's', '售' to 's', '授' to 's',
        '瘦' to 's', '书' to 's', '抒' to 's', '枢' to 's', '叔' to 's',
        '殊' to 's', '梳' to 's', '淑' to 's', '疏' to 's', '舒' to 's',
        '输' to 's', '蔬' to 's', '熟' to 's', '暑' to 's', '黍' to 's',
        '署' to 's', '蜀' to 's', '鼠' to 's', '属' to 's', '术' to 's',
        '束' to 's', '述' to 's', '树' to 's', '竖' to 's', '恕' to 's',
        '庶' to 's', '数' to 's', '术' to 's', '刷' to 's', '耍' to 's',
        '衰' to 's', '摔' to 's', '甩' to 's', '帅' to 's', '拴' to 's',
        '霜' to 's', '双' to 's', '爽' to 's', '水' to 's', '税' to 's',
        '睡' to 's', '顺' to 's', '瞬' to 's', '说' to 's', '烁' to 's',
        '朔' to 's', '硕' to 's', '丝' to 's', '司' to 's', '私' to 's',
        '思' to 's', '斯' to 's', '死' to 's', '四' to 's', '寺' to 's',
        '似' to 's', '饲' to 's', '肆' to 's', '松' to 's', '怂' to 's',
        '耸' to 's', '送' to 's', '颂' to 's', '诵' to 's', '搜' to 's',
        '艘' to 's', '苏' to 's', '俗' to 's', '诉' to 's', '肃' to 's',
        '素' to 's', '速' to 's', '宿' to 's', '塑' to 's', '溯' to 's',
        '酸' to 's', '蒜' to 's', '算' to 's', '虽' to 's', '随' to 's',
        '髓' to 's', '岁' to 's', '祟' to 's', '遂' to 's', '碎' to 's',
        '穗' to 's', '孙' to 's', '损' to 's', '笋' to 's', '蓑' to 's',
        '梭' to 's', '唆' to 's', '缩' to 's', '所' to 's', '索' to 's',
        '锁' to 's',
        // T
        '他' to 't', '它' to 't', '她' to 't', '塌' to 't', '踏' to 't',
        '塔' to 't', '胎' to 't', '台' to 't', '抬' to 't', '太' to 't',
        '泰' to 't', '贪' to 't', '摊' to 't', '滩' to 't', '瘫' to 't',
        '谈' to 't', '坛' to 't', '潭' to 't', '坦' to 't', '叹' to 't',
        '炭' to 't', '探' to 't', '糖' to 't', '躺' to 't', '趟' to 't',
        '涛' to 't', '掏' to 't', '逃' to 't', '桃' to 't', '陶' to 't',
        '萄' to 't', '淘' to 't', '讨' to 't', '套' to 't', '特' to 't',
        '疼' to 't', '腾' to 't', '梯' to 't', '踢' to 't', '啼' to 't',
        '提' to 't', '题' to 't', '蹄' to 't', '体' to 't', '替' to 't',
        '天' to 't', '添' to 't', '田' to 't', '甜' to 't', '填' to 't',
        '挑' to 't', '条' to 't', '迢' to 't', '跳' to 't', '贴' to 't',
        '铁' to 't', '帖' to 't', '厅' to 't', '听' to 't', '烃' to 't',
        '廷' to 't', '亭' to 't', '庭' to 't', '停' to 't', '挺' to 't',
        '艇' to 't', '通' to 't', '同' to 't', '桐' to 't', '铜' to 't',
        '童' to 't', '统' to 't', '桶' to 't', '筒' to 't', '痛' to 't',
        '偷' to 't', '投' to 't', '头' to 't', '透' to 't', '突' to 't',
        '图' to 't', '徒' to 't', '涂' to 't', '途' to 't', '屠' to 't',
        '土' to 't', '吐' to 't', '兔' to 't', '团' to 't', '推' to 't',
        '颓' to 't', '腿' to 't', '退' to 't', '吞' to 't', '托' to 't',
        '拖' to 't', '脱' to 't', '驮' to 't', '陀' to 't', '驼' to 't',
        '妥' to 't', '拓' to 't', '唾' to 't',
        // W
        '挖' to 'w', '哇' to 'w', '蛙' to 'w', '娃' to 'w', '瓦' to 'w',
        '袜' to 'w', '歪' to 'w', '外' to 'w', '弯' to 'w', '湾' to 'w',
        '丸' to 'w', '完' to 'w', '玩' to 'w', '顽' to 'w', '挽' to 'w',
        '晚' to 'w', '碗' to 'w', '万' to 'w', '汪' to 'w', '亡' to 'w',
        '王' to 'w', '网' to 'w', '往' to 'w', '妄' to 'w', '忘' to 'w',
        '旺' to 'w', '望' to 'w', '危' to 'w', '威' to 'w', '微' to 'w',
        '为' to 'w', '韦' to 'w', '围' to 'w', '违' to 'w', '唯' to 'w',
        '维' to 'w', '伟' to 'w', '伪' to 'w', '尾' to 'w', '纬' to 'w',
        '委' to 'w', '卫' to 'w', '未' to 'w', '位' to 'w', '味' to 'w',
        '畏' to 'w', '胃' to 'w', '谓' to 'w', '喂' to 'w', '尉' to 'w',
        '蔚' to 'w', '慰' to 'w', '魏' to 'w', '温' to 'w', '文' to 'w',
        '纹' to 'w', '蚊' to 'w', '闻' to 'w', '吻' to 'w', '稳' to 'w',
        '问' to 'w', '翁' to 'w', '窝' to 'w', '我' to 'w', '沃' to 'w',
        '卧' to 'w', '握' to 'w', '乌' to 'w', '污' to 'w', '巫' to 'w',
        '呜' to 'w', '屋' to 'w', '无' to 'w', '吴' to 'w', '吾' to 'w',
        '梧' to 'w', '五' to 'w', '午' to 'w', '伍' to 'w', '武' to 'w',
        '侮' to 'w', '捂' to 'w', '舞' to 'w', '勿' to 'w', '务' to 'w',
        '戊' to 'w', '物' to 'w', '误' to 'w', '悟' to 'w', '雾' to 'w',
        // X
        '夕' to 'x', '西' to 'x', '吸' to 'x', '希' to 'x', '昔' to 'x',
        '牺' to 'x', '息' to 'x', '惜' to 'x', '悉' to 'x', '蟋' to 'x',
        '锡' to 'x', '熙' to 'x', '嘻' to 'x', '嬉' to 'x', '膝' to 'x',
        '习' to 'x', '席' to 'x', '袭' to 'x', '洗' to 'x', '喜' to 'x',
        '戏' to 'x', '系' to 'x', '细' to 'x', '隙' to 'x', '虾' to 'x',
        '瞎' to 'x', '峡' to 'x', '狭' to 'x', '霞' to 'x', '下' to 'x',
        '夏' to 'x', '吓' to 'x', '掀' to 'x', '先' to 'x', '仙' to 'x',
        '纤' to 'x', '鲜' to 'x', '闲' to 'x', '贤' to 'x', '弦' to 'x',
        '咸' to 'x', '涎' to 'x', '衔' to 'x', '嫌' to 'x', '显' to 'x',
        '险' to 'x', '县' to 'x', '现' to 'x', '限' to 'x', '线' to 'x',
        '宪' to 'x', '陷' to 'x', '馅' to 'x', '羡' to 'x', '献' to 'x',
        '腺' to 'x', '乡' to 'x', '相' to 'x', '香' to 'x', '厢' to 'x',
        '湘' to 'x', '箱' to 'x', '详' to 'x', '祥' to 'x', '翔' to 'x',
        '享' to 'x', '响' to 'x', '想' to 'x', '向' to 'x', '巷' to 'x',
        '项' to 'x', '象' to 'x', '像' to 'x', '橡' to 'x', '削' to 'x',
        '消' to 'x', '宵' to 'x', '萧' to 'x', '硝' to 'x', '销' to 'x',
        '小' to 'x', '晓' to 'x', '孝' to 'x', '效' to 'x', '校' to 'x',
        '笑' to 'x', '些' to 'x', '歇' to 'x', '协' to 'x', '邪' to 'x',
        '胁' to 'x', '斜' to 'x', '谐' to 'x', '携' to 'x', '鞋' to 'x',
        '写' to 'x', '泄' to 'x', '泻' to 'x', '卸' to 'x', '屑' to 'x',
        '械' to 'x', '谢' to 'x', '懈' to 'x', '蟹' to 'x', '心' to 'x',
        '辛' to 'x', '欣' to 'x', '新' to 'x', '薪' to 'x', '信' to 'x',
        '兴' to 'x', '星' to 'x', '猩' to 'x', '腥' to 'x', '刑' to 'x',
        '行' to 'x', '形' to 'x', '型' to 'x', '醒' to 'x', '杏' to 'x',
        '姓' to 'x', '幸' to 'x', '性' to 'x', '凶' to 'x', '兄' to 'x',
        '匈' to 'x', '汹' to 'x', '胸' to 'x', '雄' to 'x', '熊' to 'x',
        '休' to 'x', '修' to 'x', '羞' to 'x', '朽' to 'x', '秀' to 'x',
        '绣' to 'x', '袖' to 'x', '虚' to 'x', '需' to 'x', '徐' to 'x',
        '许' to 'x', '序' to 'x', '叙' to 'x', '畜' to 'x', '绪' to 'x',
        '续' to 'x', '蓄' to 'x', '宣' to 'x', '悬' to 'x', '旋' to 'x',
        '选' to 'x', '癣' to 'x', '炫' to 'x', '眩' to 'x', '绚' to 'x',
        '靴' to 'x', '学' to 'x', '穴' to 'x', '雪' to 'x', '血' to 'x',
        // Y
        '鸭' to 'y', '牙' to 'y', '芽' to 'y', '崖' to 'y', '衙' to 'y',
        '雅' to 'y', '亚' to 'y', '咽' to 'y', '烟' to 'y', '淹' to 'y',
        '延' to 'y', '严' to 'y', '言' to 'y', '岩' to 'y', '炎' to 'y',
        '沿' to 'y', '研' to 'y', '盐' to 'y', '蜒' to 'y', '颜' to 'y',
        '掩' to 'y', '眼' to 'y', '演' to 'y', '厌' to 'y', '宴' to 'y',
        '艳' to 'y', '验' to 'y', '谚' to 'y', '焰' to 'y', '雁' to 'y',
        '燕' to 'y', '央' to 'y', '殃' to 'y', '秧' to 'y', '扬' to 'y',
        '羊' to 'y', '阳' to 'y', '杨' to 'y', '佯' to 'y', '洋' to 'y',
        '仰' to 'y', '养' to 'y', '氧' to 'y', '痒' to 'y', '样' to 'y',
        '夭' to 'y', '妖' to 'y', '腰' to 'y', '邀' to 'y', '尧' to 'y',
        '姚' to 'y', '摇' to 'y', '遥' to 'y', '窑' to 'y', '谣' to 'y',
        '咬' to 'y', '药' to 'y', '要' to 'y', '耀' to 'y', '爷' to 'y',
        '耶' to 'y', '也' to 'y', '冶' to 'y', '野' to 'y', '业' to 'y',
        '叶' to 'y', '页' to 'y', '夜' to 'y', '液' to 'y', '一' to 'y',
        '衣' to 'y', '医' to 'y', '依' to 'y', '仪' to 'y', '夷' to 'y',
        '宜' to 'y', '移' to 'y', '遗' to 'y', '疑' to 'y', '乙' to 'y',
        '已' to 'y', '以' to 'y', '矣' to 'y', '蚁' to 'y', '椅' to 'y',
        '义' to 'y', '亿' to 'y', '忆' to 'y', '艺' to 'y', '议' to 'y',
        '亦' to 'y', '异' to 'y', '役' to 'y', '抑' to 'y', '译' to 'y',
        '易' to 'y', '疫' to 'y', '益' to 'y', '谊' to 'y', '逸' to 'y',
        '意' to 'y', '溢' to 'y', '毅' to 'y', '翼' to 'y', '因' to 'y',
        '阴' to 'y', '音' to 'y', '姻' to 'y', '银' to 'y', '引' to 'y',
        '饮' to 'y', '隐' to 'y', '印' to 'y', '应' to 'y', '英' to 'y',
        '婴' to 'y', '鹰' to 'y', '迎' to 'y', '盈' to 'y', '营' to 'y',
        '蝇' to 'y', '赢' to 'y', '影' to 'y', '映' to 'y', '硬' to 'y',
        '哟' to 'y', '拥' to 'y', '佣' to 'y', '痈' to 'y', '庸' to 'y',
        '雍' to 'y', '永' to 'y', '泳' to 'y', '勇' to 'y', '涌' to 'y',
        '用' to 'y', '优' to 'y', '忧' to 'y', '悠' to 'y', '尤' to 'y',
        '由' to 'y', '犹' to 'y', '邮' to 'y', '油' to 'y', '游' to 'y',
        '友' to 'y', '有' to 'y', '又' to 'y', '右' to 'y', '幼' to 'y',
        '诱' to 'y', '于' to 'y', '予' to 'y', '余' to 'y', '鱼' to 'y',
        '娱' to 'y', '渔' to 'y', '愉' to 'y', '愚' to 'y', '榆' to 'y',
        '与' to 'y', '宇' to 'y', '羽' to 'y', '雨' to 'y', '语' to 'y',
        '玉' to 'y', '驭' to 'y', '芋' to 'y', '育' to 'y', '郁' to 'y',
        '狱' to 'y', '浴' to 'y', '预' to 'y', '域' to 'y', '欲' to 'y',
        '喻' to 'y', '寓' to 'y', '御' to 'y', '裕' to 'y', '遇' to 'y',
        '愈' to 'y', '誉' to 'y', '豫' to 'y', '冤' to 'y', '元' to 'y',
        '园' to 'y', '员' to 'y', '原' to 'y', '圆' to 'y', '袁' to 'y',
        '援' to 'y', '缘' to 'y', '源' to 'y', '远' to 'y', '怨' to 'y',
        '院' to 'y', '愿' to 'y', '曰' to 'y', '约' to 'y', '月' to 'y',
        '岳' to 'y', '悦' to 'y', '阅' to 'y', '跃' to 'y', '越' to 'y',
        '云' to 'y', '匀' to 'y', '允' to 'y', '孕' to 'y', '运' to 'y',
        '酝' to 'y', '晕' to 'y', '韵' to 'y', '蕴' to 'y',
        // Z
        '杂' to 'z', '砸' to 'z', '灾' to 'z', '栽' to 'z', '宰' to 'z',
        '载' to 'z', '再' to 'z', '在' to 'z', '咱' to 'z', '攒' to 'z',
        '暂' to 'z', '赞' to 'z', '赃' to 'z', '脏' to 'z', '葬' to 'z',
        '遭' to 'z', '糟' to 'z', '早' to 'z', '枣' to 'z', '藻' to 'z',
        '灶' to 'z', '皂' to 'z', '造' to 'z', '噪' to 'z', '燥' to 'z',
        '躁' to 'z', '则' to 'z', '责' to 'z', '择' to 'z', '泽' to 'z',
        '贼' to 'z', '怎' to 'z', '增' to 'z', '憎' to 'z', '赠' to 'z',
        '扎' to 'z', '渣' to 'z', '札' to 'z', '轧' to 'z', '闸' to 'z',
        '诈' to 'z', '榨' to 'z', '摘' to 'z', '宅' to 'z', '窄' to 'z',
        '债' to 'z', '寨' to 'z', '沾' to 'z', '斩' to 'z', '展' to 'z',
        '盏' to 'z', '崭' to 'z', '占' to 'z', '战' to 'z', '站' to 'z',
        '张' to 'z', '章' to 'z', '彰' to 'z', '樟' to 'z', '涨' to 'z',
        '掌' to 'z', '丈' to 'z', '仗' to 'z', '帐' to 'z', '账' to 'z',
        '胀' to 'z', '障' to 'z', '招' to 'z', '找' to 'z', '召' to 'z',
        '兆' to 'z', '照' to 'z', '罩' to 'z', '遮' to 'z', '折' to 'z',
        '哲' to 'z', '者' to 'z', '这' to 'z', '浙' to 'z', '针' to 'z',
        '侦' to 'z', '珍' to 'z', '真' to 'z', '诊' to 'z', '枕' to 'z',
        '阵' to 'z', '振' to 'z', '镇' to 'z', '震' to 'z', '睁' to 'z',
        '争' to 'z', '征' to 'z', '挣' to 'z', '睁' to 'z', '筝' to 'z',
        '蒸' to 'z', '整' to 'z', '正' to 'z', '证' to 'z', '郑' to 'z',
        '政' to 'z', '症' to 'z', '之' to 'z', '支' to 'z', '只' to 'z',
        '芝' to 'z', '枝' to 'z', '知' to 'z', '织' to 'z', '肢' to 'z',
        '脂' to 'z', '蜘' to 'z', '执' to 'z', '直' to 'z', '值' to 'z',
        '职' to 'z', '植' to 'z', '殖' to 'z', '止' to 'z', '旨' to 'z',
        '址' to 'z', '纸' to 'z', '指' to 'z', '至' to 'z', '志' to 'z',
        '制' to 'z', '帜' to 'z', '治' to 'z', '质' to 'z', '致' to 'z',
        '秩' to 'z', '智' to 'z', '置' to 'z', '稚' to 'z', '中' to 'z',
        '忠' to 'z', '终' to 'z', '钟' to 'z', '肿' to 'z', '种' to 'z',
        '仲' to 'z', '众' to 'z', '舟' to 'z', '州' to 'z', '周' to 'z',
        '洲' to 'z', '粥' to 'z', '轴' to 'z', '肘' to 'z', '咒' to 'z',
        '皱' to 'z', '昼' to 'z', '朱' to 'z', '株' to 'z', '珠' to 'z',
        '诸' to 'z', '猪' to 'z', '竹' to 'z', '烛' to 'z', '逐' to 'z',
        '主' to 'z', '煮' to 'z', '嘱' to 'z', '住' to 'z', '助' to 'z',
        '注' to 'z', '驻' to 'z', '柱' to 'z', '祝' to 'z', '著' to 'z',
        '筑' to 'z', '铸' to 'z', '抓' to 'z', '爪' to 'z', '专' to 'z',
        '砖' to 'z', '转' to 'z', '赚' to 'z', '庄' to 'z', '桩' to 'z',
        '装' to 'z', '壮' to 'z', '状' to 'z', '撞' to 'z', '追' to 'z',
        '准' to 'z', '捉' to 'z', '桌' to 'z', '仔' to 'z', '兹' to 'z',
        '资' to 'z', '姿' to 'z', '滋' to 'z', '紫' to 'z', '字' to 'z',
        '自' to 'z', '宗' to 'z', '综' to 'z', '踪' to 'z', '总' to 'z',
        '纵' to 'z', '走' to 'z', '奏' to 'z', '租' to 'z', '足' to 'z',
        '族' to 'z', '阻' to 'z', '组' to 'z', '祖' to 'z', '钻' to 'z',
        '嘴' to 'z', '最' to 'z', '罪' to 'z', '醉' to 'z', '尊' to 'z',
        '遵' to 'z', '昨' to 'z', '左' to 'z', '佐' to 'z', '作' to 'z',
        '坐' to 'z', '座' to 'z', '做' to 'z'
    )
    
    /**
     * 预置常用股票别名映射
     * 代码 -> 别名列表
     */
    private val stockAliases = mapOf(
        // 贵州茅台
        "600519" to listOf("茅台", "贵州茅台", "gzmt", "maotai", "guizhoumaotai"),
        // 平安银行
        "000001" to listOf("平安银行", "payh", "pinganyinhang", "平安", "pingan"),
        // 宁德时代
        "300750" to listOf("宁德时代", "ndsd", "ningdeshidai", "宁王", "ningwang"),
        // 五粮液
        "000858" to listOf("五粮液", "wly", "wuliangye"),
        // 海康威视
        "002415" to listOf("海康威视", "hkwsk", "haikangweishi", "海康", "haikang"),
        // 招商银行
        "600036" to listOf("招商银行", "zsyh", "zhaoshangyinhang", "招行", "zhaohang"),
        // 美的集团
        "000333" to listOf("美的集团", "mdjt", "meidejituan", "美的", "meidi"),
        // 恒瑞医药
        "600276" to listOf("恒瑞医药", "hryy", "hengruiyiyao", "恒瑞", "hengrui"),
        // 比亚迪
        "002594" to listOf("比亚迪", "byd", "biyadi", "比王", "biwang"),
        // 东方财富
        "300059" to listOf("东方财富", "dfcf", "dongfangcaifu", "东财", "dongcai"),
        // 中国平安
        "601318" to listOf("中国平安", "zgpa", "zhongguopingan", "平安", "pingan"),
        // 长江电力
        "600900" to listOf("长江电力", "cjdl", "changjiangdianli", "长电", "changdian"),
        // 泸州老窖
        "000568" to listOf("泸州老窖", "lzlj", "laozhoulaojiao", "老窖", "laojiao"),
        // 科大讯飞
        "002230" to listOf("科大讯飞", "kdxf", "kexunfei", "讯飞", "xunfei"),
        // 汇川技术
        "300124" to listOf("汇川技术", "hcjs", "huichuanjishu", "汇川", "huichuan"),
        // 中国中免
        "601888" to listOf("中国中免", "zgzm", "zhongguozhongmian", "中免", "zhongmian"),
        // 万华化学
        "600309" to listOf("万华化学", "whhx", "wanhuahuaxue", "万华", "wanhua"),
        // 宁波银行
        "002142" to listOf("宁波银行", "nbyh", "ningboyinhang", "宁行", "ninghang"),
        // 迈瑞医疗
        "300760" to listOf("迈瑞医疗", "mryl", "mairuiyiliao", "迈瑞", "mairui"),
        // 隆基绿能
        "601012" to listOf("隆基绿能", "ljln", "longjilvneng", "隆基", "longji"),
        // 腾讯控股 (港股)
        "00700" to listOf("腾讯", "tencent", "txkg", "tengxun", "鹅厂", "echang"),
        // 阿里巴巴 (港股)
        "09988" to listOf("阿里", "alibaba", "albaba", "阿狸"),
        // 美团 (港股)
        "03690" to listOf("美团", "meituan", "mt", "mtwp"),
        // 小米集团 (港股)
        "01810" to listOf("小米", "xiaomi", "xm", "xmjt"),
        // 京东集团 (港股)
        "09618" to listOf("京东", "jd", "jingdong", "jdjt", "狗东"),
        // 苹果 (美股)
        "AAPL" to listOf("苹果", "apple", "pg", "pingguo"),
        // 特斯拉 (美股)
        "TSLA" to listOf("特斯拉", "tesla", "tsla", "tesila", "特拉斯"),
        // 英伟达 (美股)
        "NVDA" to listOf("英伟达", "nvidia", "yingweida", "nvda"),
        // 微软 (美股)
        "MSFT" to listOf("微软", "microsoft", "weiruan", "msft"),
        // 谷歌 (美股)
        "GOOGL" to listOf("谷歌", "google", "guge", "googl", "alphabet"),
        // 亚马逊 (美股)
        "AMZN" to listOf("亚马逊", "amazon", "yamaxun", "amzn"),
        // Meta (美股)
        "META" to listOf("meta", "facebook", "feishubu", "脸书", "yuan", "元宇宙"),
        // 伯克希尔 (美股)
        "BRK.B" to listOf("伯克希尔", "brk", "berkshire", "bukexi'er", "巴菲特"),
        // 台积电 (美股)
        "TSM" to listOf("台积电", "tsmc", "taijidian", "tsm"),
        // 阿里 (美股)
        "BABA" to listOf("阿里", "alibaba", "阿里巴巴", "baba"),
        // 拼多多 (美股)
        "PDD" to listOf("拼多多", "pdd", "pinduoduo", "拼夕夕", "pxx")
    )
    
    /**
     * 获取股票别名列表
     */
    fun getAliases(symbol: String): List<String> {
        return stockAliases[symbol] ?: emptyList()
    }
    
    /**
     * 获取拼音首字母缩写（简化实现）
     * 例如："贵州茅台" -> "gzmt"
     */
    fun getPinyinAbbreviation(name: String): String {
        return name.map { char ->
            when {
                // 如果是英文字母或数字，保留原样
                char.isLetterOrDigit() -> char.toString()
                // 如果是中文字符，从映射表中获取首字母
                pinyinFirstLetters.containsKey(char) -> pinyinFirstLetters[char].toString()
                // 其他字符忽略
                else -> ""
            }
        }.joinToString("")
    }
    
    /**
     * 获取全拼（简化实现，使用常见的拼音映射）
     * 例如："贵州茅台" -> "gui zhou mao tai"
     */
    fun getPinyinFull(name: String): String {
        // 简化实现：直接返回拼音首字母的组合
        // 在实际应用中，可以使用更完整的拼音库
        return getPinyinAbbreviation(name)
    }
    
    /**
     * 检查查询是否匹配股票（支持代码、名称、拼音、别名）
     * @param query 搜索查询
     * @param symbol 股票代码
     * @param name 股票名称
     * @return 是否匹配
     */
    fun matches(query: String, symbol: String, name: String): Boolean {
        val normalizedQuery = query.trim().lowercase()
        
        // 1. 代码匹配（支持部分匹配）
        if (symbol.contains(normalizedQuery.uppercase()) || 
            symbol.contains(normalizedQuery)) {
            return true
        }
        
        // 2. 名称包含匹配
        if (name.contains(query) || name.contains(query.uppercase())) {
            return true
        }
        
        // 3. 拼音首字母匹配 (如 "gzmt" -> "贵州茅台")
        val pinyinAbbr = getPinyinAbbreviation(name).lowercase()
        if (pinyinAbbr.contains(normalizedQuery)) {
            return true
        }
        
        // 4. 别名匹配
        val aliases = getAliases(symbol)
        if (aliases.any { it.contains(query, ignoreCase = true) }) {
            return true
        }
        
        return false
    }
    
    /**
     * 计算搜索匹配得分（用于排序）
     * 得分越高越相关
     */
    fun calculateMatchScore(query: String, symbol: String, name: String): Int {
        val normalizedQuery = query.trim().lowercase()
        var score = 0
        
        // 代码精确匹配（最高分）
        if (symbol.equals(normalizedQuery, ignoreCase = true)) {
            score += 1000
        }
        // 代码开头匹配
        else if (symbol.startsWith(normalizedQuery, ignoreCase = true)) {
            score += 800
        }
        // 代码包含匹配
        else if (symbol.contains(normalizedQuery, ignoreCase = true)) {
            score += 600
        }
        
        // 名称精确匹配
        if (name.equals(query, ignoreCase = true)) {
            score += 900
        }
        // 名称开头匹配
        else if (name.startsWith(query, ignoreCase = true)) {
            score += 700
        }
        // 名称包含匹配
        else if (name.contains(query, ignoreCase = true)) {
            score += 500
        }
        
        // 拼音首字母精确匹配
        val pinyinAbbr = getPinyinAbbreviation(name).lowercase()
        if (pinyinAbbr.equals(normalizedQuery)) {
            score += 850
        }
        // 拼音首字母开头匹配
        else if (pinyinAbbr.startsWith(normalizedQuery)) {
            score += 650
        }
        // 拼音首字母包含匹配
        else if (pinyinAbbr.contains(normalizedQuery)) {
            score += 400
        }
        
        // 别名匹配
        val aliases = getAliases(symbol)
        if (aliases.any { it.equals(query, ignoreCase = true) }) {
            score += 750
        } else if (aliases.any { it.startsWith(query, ignoreCase = true) }) {
            score += 550
        } else if (aliases.any { it.contains(query, ignoreCase = true) }) {
            score += 350
        }
        
        return score
    }
}

/**
 * 搜索结果项，包含匹配得分
 */
data class SearchResultItem(
    val symbol: String,
    val name: String,
    val matchScore: Int
) : Comparable<SearchResultItem> {
    override fun compareTo(other: SearchResultItem): Int {
        // 按匹配得分降序排列
        return other.matchScore.compareTo(this.matchScore)
    }
}
