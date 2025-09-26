package cn.javen.tool.reversegeo;

/**
 * @Description
 * @Author: Javen
 * @CreateTime: 2025/9/23 15:59
 */

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TsvToCsvConverter {

    public static void convert(String inputPath, String outputPath) throws Exception {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        new FileInputStream(inputPath), StandardCharsets.UTF_8));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(outputPath), StandardCharsets.UTF_8))
        ) {
            String line;
            boolean isFirst = true;

            while ((line = reader.readLine()) != null) {
                // 按制表符分割
                String[] fields = line.split("\t", -1); // -1 保留末尾空字段

                // 转义字段：如果含逗号、换行、双引号，则用双引号包裹
                String[] escaped = Arrays.stream(fields)
                        .map(TsvToCsvConverter::escapeField)
                        .toArray(String[]::new);

                // 用逗号连接
                String csvLine = String.join(",", escaped);
                writer.write(csvLine);
                if (isFirst) isFirst = false;
                else writer.newLine();
            }
        }
    }

    private static String escapeField(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            // 转义双引号为 ""
            field = field.replace("\"", "\"\"");
            return "\"" + field + "\"";
        }
        return field;
    }

    public static void main(String[] args) throws Exception {
        convert("/Users/work/app/code/reverse-geo/src/main/resources/street_data.csv", "/Users/work/app/code/reverse-geo/src/main/resources/streets_over.csv");
        System.out.println("转换完成");
    }
}