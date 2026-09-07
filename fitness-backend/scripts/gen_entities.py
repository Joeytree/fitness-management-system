# -*- coding: utf-8 -*-
"""批量生成 entity 与 mapper 代码"""
import os

BASE = r"D:/Workb_pj/fitness-backend/src/main/java/com/fitness"
ENTITY_DIR = os.path.join(BASE, "entity")
MAPPER_DIR = os.path.join(BASE, "mapper")
os.makedirs(ENTITY_DIR, exist_ok=True)
os.makedirs(MAPPER_DIR, exist_ok=True)

# 表定义：(类名, 表名, [(java类型, 字段名), ...])
tables = [
    ("User", "user", [
        ("Long", "id"), ("String", "phone"), ("String", "password"),
        ("String", "nickname"), ("String", "avatar"), ("Integer", "gender"),
        ("LocalDate", "birthday"), ("BigDecimal", "height"), ("BigDecimal", "targetWeight"),
        ("BigDecimal", "targetBodyFat"),
        ("Integer", "goal"),
        ("Integer", "level"), ("String", "tags"), ("Integer", "status"),
        ("LocalDateTime", "createTime"), ("LocalDateTime", "updateTime"), ("Integer", "deleted"),
    ]),
    ("Role", "role", [
        ("Long", "id"), ("String", "name"), ("String", "code"),
        ("String", "description"), ("LocalDateTime", "createTime"), ("LocalDateTime", "updateTime"),
    ]),
    ("Permission", "permission", [
        ("Long", "id"), ("String", "name"), ("String", "code"),
        ("Integer", "type"), ("Long", "parentId"), ("String", "path"),
        ("Integer", "sort"), ("LocalDateTime", "createTime"),
    ]),
    ("RolePermission", "role_permission", [
        ("Long", "id"), ("Long", "roleId"), ("Long", "permissionId"),
    ]),
    ("Admin", "admin", [
        ("Long", "id"), ("String", "username"), ("String", "password"),
        ("String", "nickname"), ("Long", "roleId"), ("Integer", "status"),
        ("LocalDateTime", "createTime"), ("LocalDateTime", "updateTime"), ("Integer", "deleted"),
    ]),
    ("Course", "course", [
        ("Long", "id"), ("String", "name"), ("String", "category"),
        ("String", "cover"), ("String", "intro"), ("Integer", "difficulty"),
        ("Integer", "duration"), ("Integer", "calorie"), ("Integer", "status"),
        ("Integer", "viewCount"), ("Integer", "favoriteCount"), ("Integer", "likeCount"),
        ("LocalDateTime", "createTime"), ("LocalDateTime", "updateTime"), ("Integer", "deleted"),
    ]),
    ("CourseFavorite", "course_favorite", [
        ("Long", "id"), ("Long", "courseId"), ("Long", "userId"),
        ("LocalDateTime", "createTime"),
    ]),
    ("CourseAction", "course_action", [
        ("Long", "id"), ("Long", "courseId"), ("Long", "actionId"), ("Integer", "sort"),
    ]),
    ("CourseLike", "course_like", [
        ("Long", "id"), ("Long", "courseId"), ("Long", "userId"),
        ("LocalDateTime", "createTime"),
    ]),
    ("Action", "action", [
        ("Long", "id"), ("String", "name"), ("String", "part"),
        ("Integer", "difficulty"), ("String", "equipment"), ("String", "steps"),
        ("String", "tips"), ("String", "errors"), ("String", "breath"),
        ("Integer", "mediaType"), ("String", "mediaUrl"), ("Integer", "status"),
        ("String", "rejectReason"),
        ("LocalDateTime", "createTime"), ("LocalDateTime", "updateTime"), ("Integer", "deleted"),
    ]),
    ("TrainingPlan", "training_plan", [
        ("Long", "id"), ("Long", "userId"), ("String", "name"),
        ("Integer", "goal"), ("Integer", "level"), ("String", "cycle"),
        ("String", "tags"), ("Integer", "isTemplate"),
        ("LocalDateTime", "createTime"), ("LocalDateTime", "updateTime"), ("Integer", "deleted"),
    ]),
    ("PlanAction", "plan_action", [
        ("Long", "id"), ("Long", "planId"), ("Long", "actionId"),
        ("Integer", "sets"), ("Integer", "reps"), ("Integer", "rest"), ("Integer", "sort"),
    ]),
    ("Food", "food", [
        ("Long", "id"), ("String", "name"), ("String", "category"),
        ("BigDecimal", "calorie"), ("BigDecimal", "protein"), ("BigDecimal", "carb"),
        ("BigDecimal", "fat"), ("String", "unit"), ("Integer", "status"),
        ("LocalDateTime", "createTime"), ("LocalDateTime", "updateTime"), ("Integer", "deleted"),
    ]),
    ("Recipe", "recipe", [
        ("Long", "id"), ("String", "name"), ("Integer", "goal"),
        ("String", "tags"), ("String", "content"), ("Integer", "calorie"),
        ("BigDecimal", "protein"), ("BigDecimal", "carb"), ("BigDecimal", "fat"),
        ("String", "cover"), ("Integer", "status"),
        ("LocalDateTime", "createTime"), ("LocalDateTime", "updateTime"), ("Integer", "deleted"),
    ]),
    ("DietRecord", "diet_record", [
        ("Long", "id"), ("Long", "userId"), ("Integer", "mealType"),
        ("Long", "foodId"), ("BigDecimal", "amount"), ("LocalDate", "recordDate"),
        ("LocalDateTime", "createTime"),
    ]),
    ("BodyData", "body_data", [
        ("Long", "id"), ("Long", "userId"), ("BigDecimal", "weight"),
        ("BigDecimal", "bodyFat"), ("BigDecimal", "bmi"), ("BigDecimal", "chest"),
        ("BigDecimal", "waist"), ("BigDecimal", "hip"), ("BigDecimal", "arm"),
        ("BigDecimal", "muscle"), ("BigDecimal", "water"), ("LocalDate", "recordDate"),
        ("LocalDateTime", "createTime"),
    ]),
    ("CheckIn", "check_in", [
        ("Long", "id"), ("Long", "userId"), ("Long", "planId"),
        ("LocalDate", "checkDate"), ("Integer", "duration"), ("Integer", "calorie"),
        ("LocalDateTime", "createTime"),
    ]),
    ("CourseComment", "course_comment", [
        ("Long", "id"), ("Long", "courseId"), ("Long", "userId"),
        ("String", "content"), ("Integer", "rating"), ("Integer", "status"),
        ("String", "rejectReason"),
        ("LocalDateTime", "createTime"),
    ]),
    ("Follow", "follow", [
        ("Long", "id"), ("Long", "userId"), ("Long", "followUserId"),
        ("LocalDateTime", "createTime"),
    ]),
    ("Moment", "moment", [
        ("Long", "id"), ("Long", "userId"), ("String", "content"),
        ("String", "images"), ("Integer", "status"), ("String", "rejectReason"),
        ("LocalDateTime", "createTime"), ("LocalDateTime", "updateTime"), ("Integer", "deleted"),
    ]),
    ("MomentLike", "moment_like", [
        ("Long", "id"), ("Long", "momentId"), ("Long", "userId"),
        ("LocalDateTime", "createTime"),
    ]),
    ("MomentComment", "moment_comment", [
        ("Long", "id"), ("Long", "momentId"), ("Long", "userId"),
        ("String", "content"), ("Long", "replyTo"), ("Integer", "status"),
        ("String", "rejectReason"),
        ("LocalDateTime", "createTime"),
    ]),
    ("RecommendRecord", "recommend_record", [
        ("Long", "id"), ("Long", "userId"), ("Integer", "type"),
        ("Long", "targetId"), ("Integer", "score"), ("Integer", "clicked"),
        ("Integer", "adopted"), ("LocalDateTime", "createTime"),
    ]),
    ("Article", "article", [
        ("Long", "id"), ("String", "title"), ("Integer", "type"),
        ("String", "content"), ("String", "cover"), ("Integer", "status"),
        ("LocalDateTime", "createTime"), ("LocalDateTime", "updateTime"), ("Integer", "deleted"),
    ]),
]

EXTRA_IMPORTS = {
    "BigDecimal": "import java.math.BigDecimal;",
    "LocalDate": "import java.time.LocalDate;",
    "LocalDateTime": "import java.time.LocalDateTime;",
}


def gen_entity(cls, table, fields):
    imports = set()
    for typ, _ in fields:
        if typ in EXTRA_IMPORTS:
            imports.add(EXTRA_IMPORTS[typ])
    import_lines = "\n".join(sorted(imports))

    lines = []
    lines.append("package com.fitness.entity;")
    lines.append("")
    lines.append("import com.baomidou.mybatisplus.annotation.IdType;")
    lines.append("import com.baomidou.mybatisplus.annotation.TableId;")
    lines.append("import com.baomidou.mybatisplus.annotation.TableLogic;")
    lines.append("import com.baomidou.mybatisplus.annotation.TableName;")
    lines.append("import lombok.Data;")
    if import_lines:
        lines.append(import_lines)
    lines.append("")
    lines.append("/**")
    lines.append(" * %s 表实体" % table)
    lines.append(" */")
    lines.append("@Data")
    lines.append("@TableName(\"%s\")" % table)
    lines.append("public class %s {" % cls)
    lines.append("")
    for typ, name in fields:
        if name == "id":
            lines.append("    @TableId(type = IdType.AUTO)")
            lines.append("    private %s id;" % typ)
        elif name == "deleted":
            lines.append("    @TableLogic")
            lines.append("    private Integer deleted;")
        else:
            lines.append("    private %s %s;" % (typ, name))
    lines.append("")
    lines.append("}")
    return "\n".join(lines) + "\n"


def gen_mapper(cls):
    return (
        "package com.fitness.mapper;\n\n"
        "import com.baomidou.mybatisplus.core.mapper.BaseMapper;\n"
        "import com.fitness.entity.%s;\n\n"
        "public interface %sMapper extends BaseMapper<%s> {\n}\n" % (cls, cls, cls)
    )


count = 0
for cls, table, fields in tables:
    entity_file = os.path.join(ENTITY_DIR, cls + ".java")
    mapper_file = os.path.join(MAPPER_DIR, cls + "Mapper.java")
    with open(entity_file, "w", encoding="utf-8") as f:
        f.write(gen_entity(cls, table, fields))
    with open(mapper_file, "w", encoding="utf-8") as f:
        f.write(gen_mapper(cls))
    count += 1

print("生成完成，共 %d 个实体 + %d 个 Mapper" % (count, count))
