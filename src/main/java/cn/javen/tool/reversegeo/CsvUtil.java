package cn.javen.tool.reversegeo;

/**
 * @Description
 * @Author: Javen
 * @CreateTime: 2025/9/25 14:46
 */

import com.opencsv.*;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.opencsv.exceptions.CsvValidationException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
public class CsvUtil {

    /**
     * 读取 CSV 文件为 List<String[]>
     *
     * @param filePath 文件路径
     * @param separator 分隔符，默认为 ,
     * @return List<String[]>，每行为一个字符串数组
     */
    public static List<String[]> read(String filePath, char separator) {
        List<String[]> result = new ArrayList<>();
        // UTF_8
        try (Reader reader = new InputStreamReader(new FileInputStream(filePath), "GBK");
             CSVReader csvReader = new CSVReaderBuilder(reader)
                     .withCSVParser(new CSVParserBuilder().withSeparator(separator).build())
                     .build()) {

            String[] line;
            while ((line = csvReader.readNext()) != null) {
                result.add(line);
            }
        } catch (IOException | CsvValidationException e) {
            throw new RuntimeException("读取 CSV 文件失败: " + filePath, e);
        }
        return result;
    }

    /**
     * 读取 CSV 文件（默认逗号分隔）
     */
    public static List<String[]> read(String filePath) {
        return read(filePath, ',');
    }

    /**
     * 将 List<String[]> 写入 CSV 文件
     *
     * @param filePath 文件路径
     * @param data 数据列表，每行为一个字符串数组
     * @param separator 分隔符
     */
    public static void write(String filePath, List<String[]> data, char separator) {
        // UTF_8
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(filePath), "GBK");
             CSVWriter csvWriter = new CSVWriter(writer, separator,
                     CSVWriter.NO_QUOTE_CHARACTER,
                     CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                     CSVWriter.DEFAULT_LINE_END)) {

            csvWriter.writeAll(data);
        } catch (IOException e) {
            throw new RuntimeException("写入 CSV 文件失败: " + filePath, e);
        }
    }

    /**
     * 写入 CSV（默认逗号分隔）
     */
    public static void write(String filePath, List<String[]> data) {
        write(filePath, data, ',');
    }

    /**
     * 将 POJO 列表写入 CSV（基于字段顺序）
     *
     * @param filePath 文件路径
     * @param data POJO 列表
     * @param clazz POJO 类型
     * @param headers 表头（可为 null）
     * @param separator 分隔符
     * @param <T> 泛型类型
     */
    public static <T> void writeBeans(String filePath, List<T> data, Class<T> clazz,
                                      String[] headers, char separator) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {

            ColumnPositionMappingStrategy<T> strategy = new ColumnPositionMappingStrategy<>();
            strategy.setType(clazz);
            if (headers != null) {
                strategy.setColumnMapping(headers);
            }

            StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(writer)
                    .withMappingStrategy(strategy)
                    .withSeparator(separator)
                    .build();

            beanToCsv.write(data);
        } catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            throw new RuntimeException("写入 POJO 到 CSV 失败: " + filePath, e);
        }
    }

    /**
     * 写入 POJO 列表（默认逗号分隔，无表头）
     */
    public static <T> void writeBeans(String filePath, List<T> data, Class<T> clazz) {
        writeBeans(filePath, data, clazz, null, ',');
    }

    /**
     * 写入 POJO 列表并指定表头和分隔符
     */
    public static <T> void writeBeansWithHeader(String filePath, List<T> data,
                                                Class<T> clazz, String[] headers) {
        writeBeans(filePath, data, clazz, headers, ',');
    }

    /**
     * 从 CSV 文件读取数据并映射为 POJO 列表
     *
     * @param filePath 文件路径
     * @param clazz POJO 类型
     * @param separator 分隔符
     * @param hasHeader 是否包含表头
     * @param <T> 泛型类型
     * @return POJO 列表
     */
    public static <T> List<T> readBeans(String filePath, Class<T> clazz, char separator, boolean hasHeader) {
        try (Reader reader = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8)) {
            CsvToBeanBuilder<T> builder = new CsvToBeanBuilder<T>(reader)
                    .withType(clazz)
                    .withSeparator(separator)
                    .withIgnoreLeadingWhiteSpace(true);

            if (hasHeader) {
                builder.withIgnoreQuotations(true);
            } else {
                // 如果没有表头，使用位置映射
                ColumnPositionMappingStrategy<T> strategy = new ColumnPositionMappingStrategy<>();
                strategy.setType(clazz);
                builder.withMappingStrategy(strategy);
            }

            return builder.build().parse();
        } catch (IOException e) {
            throw new RuntimeException("读取 CSV 到 POJO 失败: " + filePath, e);
        }
    }

    /**
     * 读取 CSV 映射为 POJO（默认逗号分隔，有表头）
     */
    public static <T> List<T> readBeans(String filePath, Class<T> clazz) {
        return readBeans(filePath, clazz, ',', true);
    }
}