package com.customerservice.config;

import com.customerservice.entity.Knowledge;
import com.customerservice.entity.KnowledgeCategory;
import com.customerservice.entity.SysUser;
import com.customerservice.mapper.KnowledgeCategoryMapper;
import com.customerservice.mapper.KnowledgeMapper;
import com.customerservice.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final KnowledgeCategoryMapper categoryMapper;
    private final KnowledgeMapper knowledgeMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        seedUsers();
        seedKnowledge();
    }

    private void seedUsers() {
        if (userMapper.selectCount(null) > 0) {
            return;
        }
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(encoder.encode("admin123"));
        admin.setNickname("系统管理员");
        admin.setRole("ADMIN");
        admin.setStatus(1);
        userMapper.insert(admin);

        SysUser agent = new SysUser();
        agent.setUsername("agent01");
        agent.setPassword(encoder.encode("123456"));
        agent.setNickname("客服小美");
        agent.setRole("AGENT");
        agent.setStatus(1);
        userMapper.insert(agent);

        log.info("已初始化默认账号：admin/admin123、agent01/123456");
    }

    private void seedKnowledge() {
        if (categoryMapper.selectCount(null) > 0 || knowledgeMapper.selectCount(null) > 0) {
            return;
        }
        Long c1 = createCategory("订单与物流", 1);
        Long c2 = createCategory("退换货", 2);
        Long c3 = createCategory("支付与发票", 3);
        Long c4 = createCategory("账号相关", 4);

        addKnowledge(c1, "如何查询我的订单？", "您可以在“我的订单”页面查看所有订单及实时状态，也可在订单详情中查看物流跟踪信息。", "订单,查询,物流,快递");
        addKnowledge(c1, "下单后什么时候发货？", "正常情况下，我们会在您下单后 48 小时内完成发货，节假日可能稍有延迟。", "发货,时间,多久,什么时候");
        addKnowledge(c1, "物流信息不更新怎么办？", "物流信息一般在揽收后 24 小时内更新，如长时间未更新请联系在线客服为您核实。", "物流,快递,不更新");
        addKnowledge(c2, "如何申请退货？", "请在“我的订单”中选择对应订单，点击“申请售后”并按提示填写原因，审核通过后即可寄回商品。", "退货,申请,售后");
        addKnowledge(c2, "退款多久能到账？", "退款一般在审核通过后的 1-7 个工作日内原路退回，具体到账时间以银行或支付平台为准。", "退款,到账,多久,钱");
        addKnowledge(c2, "退货运费由谁承担？", "因商品质量问题产生的退货运费由我方承担；因个人原因退货，运费需由买家承担。", "退货运费,运费,承担,邮费");
        addKnowledge(c3, "支持哪些支付方式？", "我们支持支付宝、微信支付以及银联银行卡支付，下单结算页可选择您方便的支付方式。", "支付,付款,支付宝,微信,银行卡");
        addKnowledge(c3, "如何开具发票？", "下单时可在结算页勾选“开具发票”并填写抬头信息，电子发票会在订单完成后发送至您的邮箱。", "发票,开票,抬头");
        addKnowledge(c4, "忘记密码怎么办？", "您可以在登录页点击“忘记密码”，通过绑定的手机号或邮箱验证后重置密码。", "密码,忘记,找回,登录,重置");
        addKnowledge(c4, "如何联系人工客服？", "在聊天窗口点击“转人工”按钮，客服会在工作时间尽快接入为您服务。", "人工,客服,电话,联系,转人工");

        log.info("已初始化示例知识库数据");
    }

    private Long createCategory(String name, int sort) {
        KnowledgeCategory category = new KnowledgeCategory();
        category.setName(name);
        category.setSort(sort);
        categoryMapper.insert(category);
        return category.getId();
    }

    private void addKnowledge(Long categoryId, String question, String answer, String keywords) {
        Knowledge knowledge = new Knowledge();
        knowledge.setCategoryId(categoryId);
        knowledge.setQuestion(question);
        knowledge.setAnswer(answer);
        knowledge.setKeywords(keywords);
        knowledge.setStatus(1);
        knowledge.setHits(0);
        knowledgeMapper.insert(knowledge);
    }
}
