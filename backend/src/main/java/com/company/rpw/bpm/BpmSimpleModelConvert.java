package com.company.rpw.bpm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

/**
 * 将「简易流程」(SIMPLE) 的 simpleModel 节点树转换为可被 Flowable 部署的 BPMN 2.0 XML。
 *
 * <p>节点类型（与前端 BpmNodeTypeEnum 对齐）：
 * <ul>
 *   <li>1  END_EVENT_NODE        → 结束事件</li>
 *   <li>10 START_USER_NODE       → 开始事件</li>
 *   <li>12 COPY_TASK_NODE        → 抄送（仅通知，BPMN 中不生成节点，透传）</li>
 *   <li>13 TRANSACTOR_NODE       → 用户任务（审批/办理）</li>
 *   <li>51 CONDITION_BRANCH_NODE → 排他网关（带条件）</li>
 *   <li>52 PARALLEL_BRANCH_NODE  → 并行网关</li>
 *   <li>53 INCLUSIVE_BRANCH_NODE → 包容网关</li>
 * </ul>
 *
 * <p>审批人策略（candidateStrategy）映射：
 * <ul>
 *   <li>30 USER        → flowable:candidateUsers（逗号分隔的用户 ID）</li>
 *   <li>60 EXPRESSION  → flowable:assignee（UEL 表达式）</li>
 *   <li>10 ROLE / 20 DEPT_MEMBER / 21 DEPT_LEADER / 22 POST / 40 USER_GROUP → flowable:candidateGroups</li>
 *   <li>其它           → 不设置办理人（需运行时手动指派）</li>
 * </ul>
 *
 * <p>注意：Flowable 部署并不强制要求 BPMNDI 布局信息，故此处生成的 XML 不含 DI，
 * 可正常部署；流程设计器/查看器在导入时会使用默认布局。
 */
public class BpmSimpleModelConvert {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 节点类型
    private static final int END_EVENT = 1;
    private static final int START_USER = 10;
    private static final int COPY_TASK = 12;
    private static final int TRANSACTOR = 13;
    private static final int CONDITION_BRANCH = 51;
    private static final int PARALLEL_BRANCH = 52;
    private static final int INCLUSIVE_BRANCH = 53;

    // 审批人策略
    private static final int STRATEGY_USER = 30;
    private static final int STRATEGY_EXPRESSION = 60;

    /**
     * 将 simpleModel JSON 转为 BPMN XML。simpleModel 为空或结构非法时返回 null。
     */
    public static String convertBpmnXml(String modelKey, String modelName, String simpleModelJson) {
        if (!StringUtils.hasText(simpleModelJson)) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(simpleModelJson);
            Builder builder = new Builder();
            Pair chain = builder.build(root);
            if (chain == null) {
                return null;
            }
            String processId = StringUtils.hasText(modelKey) ? modelKey : ("Process_" + System.currentTimeMillis());
            String processName = StringUtils.hasText(modelName) ? modelName : processId;
            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<bpmn2:definitions xmlns:bpmn2=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n"
                    + "  xmlns:flowable=\"http://flowable.org/bpmn\"\n"
                    + "  id=\"diagram_" + escape(processId) + "\"\n"
                    + "  targetNamespace=\"http://flowable.org/bpmn\">\n"
                    + "  <bpmn2:process id=\"" + escape(processId) + "\" name=\"" + escape(processName) + "\" isExecutable=\"true\">\n"
                    + builder.elements
                    + "  </bpmn2:process>\n"
                    + "</bpmn2:definitions>";
            return xml;
        } catch (Exception e) {
            return null;
        }
    }

    private static class Pair {
        final String entry;
        final String exit;

        Pair(String entry, String exit) {
            this.entry = entry;
            this.exit = exit;
        }
    }

    private static class Builder {
        final StringBuilder elements = new StringBuilder();
        int seq = 0;

        Pair build(JsonNode node) {
            if (node == null || node.isNull()) {
                return null;
            }
            int type = node.path("type").asInt(-1);
            String id = node.path("id").asText(null);
            if (id == null) {
                id = "node_" + (seq++);
            }

            // 抄送节点：BPMN 中不生成元素，直接透传其 childNode
            if (type == COPY_TASK) {
                JsonNode child = node.get("childNode");
                return build(child);
            }

            switch (type) {
                case START_USER: {
                    elements.append("    <bpmn2:startEvent id=\"").append(escape(id)).append("\"/>\n");
                    return linkChild(id, id, node.get("childNode"));
                }
                case TRANSACTOR: {
                    String name = node.path("name").asText(id);
                    String candidate = candidateAttr(node);
                    elements.append("    <bpmn2:userTask id=\"").append(escape(id)).append("\" name=\"")
                            .append(escape(name)).append("\"").append(candidate).append("/>\n");
                    return linkChild(id, id, node.get("childNode"));
                }
                case END_EVENT: {
                    elements.append("    <bpmn2:endEvent id=\"").append(escape(id)).append("\"/>\n");
                    return new Pair(id, id);
                }
                case CONDITION_BRANCH:
                case PARALLEL_BRANCH:
                case INCLUSIVE_BRANCH: {
                    String gType = gatewayType(type);
                    String splitId = id + "_split";
                    String joinId = id + "_join";
                    elements.append("    <bpmn2:").append(gType).append(" id=\"").append(escape(splitId)).append("\"/>\n");
                    elements.append("    <bpmn2:").append(gType).append(" id=\"").append(escape(joinId)).append("\"/>\n");
                    JsonNode conditionNodes = node.get("conditionNodes");
                    int branchCount = conditionNodes != null && conditionNodes.isArray() ? conditionNodes.size() : 0;
                    if (branchCount > 0) {
                        for (int i = 0; i < branchCount; i++) {
                            JsonNode cn = conditionNodes.get(i);
                            JsonNode branchChild = cn != null ? cn.get("childNode") : null;
                            Pair branch = build(branchChild);
                            if (branch == null) {
                                continue;
                            }
                            String condition = null;
                            if (type == CONDITION_BRANCH && i < branchCount - 1) {
                                String expr = cn.path("conditionExpression").asText(null);
                                if (StringUtils.hasText(expr)) {
                                    condition = expr;
                                }
                            }
                            addFlow(splitId, branch.entry, condition);
                            addFlow(branch.exit, joinId, null);
                        }
                    } else {
                        // 没有分支时，直接连通 split->join
                        addFlow(splitId, joinId, null);
                    }
                    // 分支之后的 continuation（branch.childNode）从 join 网关继续连接
                    Pair cont = linkChild(joinId, joinId, node.get("childNode"));
                    if (cont == null) {
                        return new Pair(splitId, joinId);
                    }
                    return new Pair(splitId, cont.exit);
                }
                default:
                    // 未知节点类型：当作透传处理
                    return linkChild(id, id, node.get("childNode"));
            }
        }

        private Pair linkChild(String myId, String exitIfNoChild, JsonNode child) {
            if (child == null || child.isNull()) {
                return new Pair(myId, exitIfNoChild);
            }
            Pair childPair = build(child);
            if (childPair == null) {
                return new Pair(myId, exitIfNoChild);
            }
            addFlow(myId, childPair.entry, null);
            return new Pair(myId, childPair.exit);
        }

        private void addFlow(String from, String to, String condition) {
            String fid = "flow_" + (seq++);
            elements.append("    <bpmn2:sequenceFlow id=\"").append(fid).append("\" sourceRef=\"")
                    .append(escape(from)).append("\" targetRef=\"").append(escape(to)).append("\"");
            if (StringUtils.hasText(condition)) {
                elements.append(">\n      <bpmn2:conditionExpression xsi:type=\"bpmn2:tFormalExpression\"")
                        .append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">")
                        .append(escape(condition)).append("</bpmn2:conditionExpression>\n    </bpmn2:sequenceFlow>\n");
            } else {
                elements.append("/>\n");
            }
        }

        private String gatewayType(int type) {
            if (type == PARALLEL_BRANCH) return "parallelGateway";
            if (type == INCLUSIVE_BRANCH) return "inclusiveGateway";
            return "exclusiveGateway";
        }

        private String candidateAttr(JsonNode node) {
            int strategy = node.path("candidateStrategy").asInt(-1);
            String param = node.path("candidateParam").asText(null);
            if (!StringUtils.hasText(param)) {
                return "";
            }
            if (strategy == STRATEGY_USER) {
                return " flowable:candidateUsers=\"" + escape(param) + "\"";
            }
            if (strategy == STRATEGY_EXPRESSION) {
                return " flowable:assignee=\"" + escape(param) + "\"";
            }
            // 角色/部门/岗位/用户组 等：作为候选组
            if (strategy == 10 || strategy == 20 || strategy == 21
                    || strategy == 22 || strategy == 23 || strategy == 40) {
                return " flowable:candidateGroups=\"" + escape(param) + "\"";
            }
            return "";
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
