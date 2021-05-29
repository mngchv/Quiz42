package itis.parsing2;

import itis.parsing2.annotations.Concatenate;
import itis.parsing2.annotations.NotBlank;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

public class FactoryParsingServiceImpl implements FactoryParsingService {

    @Override
    public Factory parseFactoryData(String factoryDataDirectoryPath) throws FactoryParsingException
    {
        Map<String, String> dataMap = new HashMap();
        File directory = new File(Paths.get(factoryDataDirectoryPath).toUri());
        File[] files = directory.listFiles();
        for (File file: files) {
            try {
                dataMap.putAll(getPropsFromFile(file));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        try
        {
            return parseFactoryFromMap(dataMap);
        }
        catch (FactoryParsingException e)
        {
            System.err.println(e.getMessage());
            System.err.println(e.getValidationErrors());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private Map<String, String> getPropsFromFile (File file) throws FileNotFoundException {
        Scanner scanner = new Scanner(file);

        Map<String, String> mapOfProps = new HashMap<>();
        while (scanner.hasNext())
        {
            String line = scanner.nextLine();
            if (!line.equals("---"))
            {
                String[] parts = line.split(":");
                String key = parts[0].replaceAll("\"", "").trim();
                String value = parts.length > 1 ?parts[1].replaceAll("\"", "").trim() : "";
                mapOfProps.put(key, value);
            }
        }

        return mapOfProps;
    }

    private Factory parseFactoryFromMap(Map<String, String> propsMap) throws IllegalAccessException {
        Class<Factory> factoryClass = Factory.class;

        Factory factory = null;
        try
        {
            factory = factoryClass.newInstance();
        }
        catch (InstantiationException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }

        Field[] declaredFields = factoryClass.getDeclaredFields();

        List<FactoryParsingException.FactoryValidationError> errors = new ArrayList<>();
        for (Field field : declaredFields)
        {
            field.setAccessible(true);

            NotBlank notBlank = field.getDeclaredAnnotation(NotBlank.class);

            String realName = field.getName();


            String strValue = propsMap.get(realName);

            boolean hasErrorsForCurrentField = true;

            if (notBlank != null && strValue.equals(""))
            {
                errors.add(
                        new FactoryParsingException.FactoryValidationError(field.getName(), "Поле пустое")
                );
            }
            else if (strValue == null)
            {
                errors.add(
                        new FactoryParsingException.FactoryValidationError(field.getName(), "Данных нет")
                );
            }
            else
            {
                hasErrorsForCurrentField = false;
            }

            if (!hasErrorsForCurrentField)
            {
                Class isRightClass = field.getType();
                field.set(factory, castToClass(strValue, isRightClass));
            }
        }

        if (errors.size() > 0)
        {
            throw new FactoryParsingException("Ошибка парсинга - ", errors);
        } else {
            return factory;
        }
    }
    private Object castToClass (String strValue, Class c)
    {
        if (strValue == null || strValue.equals("null"))
        {
            return null;
        }
        else if (c == LocalDate.class)
        {
            return LocalDate.parse(strValue);
        }
        return strValue;
    }


    private void concatenateExist(Field f, Factory factory, Map<String, String> map) {
        Concatenate c = f.getAnnotation(Concatenate.class);
        String s = "";

        for (String fieldName : c.fieldNames()) {
            if (map.containsKey(fieldName)) {
                s += map.get(fieldName) + c.delimiter();
            } else {
                //не успел
            }
        }
    }
}