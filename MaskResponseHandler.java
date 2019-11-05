import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.beanutils.PropertyUtils.setProperty;

@Order(1)
@ControllerAdvice
public class MaskResponseHandler implements ResponseBodyAdvice {
    private static final String MASK_SUFFIX = ".mask";
    private static final String LIST_SIGN = "\\(List\\)\\.";
    private static final String REG_SIGN = "::";
    private Environment env;

    @Autowired
    public MaskResponseHandler(Environment env) {
        this.env = env;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        if (returnType == null) {
            return false;
        }
        Method m = returnType.getMethod();
        String prop = m.getDeclaringClass().getName() + "." + m.getName() + MASK_SUFFIX;
        return Boolean.parseBoolean(env.getProperty(prop));
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof Collection) {
            for (Object obj : (Collection)body) {
                maskObject(obj);
            }
        } else {
            maskObject(body);
        }
        return body;
    }

    /**
     * Mask Object
     * @param body the object to be masked
     * @return the masked object
     */
    private void maskObject(Object body) {
        String fields = env.getProperty(body.getClass().getName() + MASK_SUFFIX);
        if (!StringUtil.isBlank(fields)) {
            for (String field : fields.split(",")) {
                String[] formats = field.split(REG_SIGN);
                String fieldName = formats[0];
                String[] splitedFieldName = fieldName.split(LIST_SIGN);
                if (formats.length == 1) {
                    maskField(body, splitedFieldName, null);
                } else {
                    String[] modifiedFormats = Arrays.copyOfRange(formats, 1, formats.length);
                    for (String modifiedFormat : modifiedFormats) {
                        maskField(body, splitedFieldName, modifiedFormat);
                    }
                }
            }
        }
    }


    /**
     * Mask Field value
     *
     * @param object    field object
     * @param fieldName field name
     * @param regex     regex to match
     */
    private void maskField(Object object, String[] fieldName, String regex) {
        if (object == null || fieldName == null || fieldName.length == 0) {
            return;
        }
        Object value;
        try {
            value = PropertyUtils.getProperty(object, fieldName[0]);
        } catch (Exception ignored) {
            return;
        }

        if (value instanceof String) {
            if (StringUtil.isBlank(regex)) {
                try {
                    setProperty(object, fieldName[0], MaskUtil.maskString((String)value));
                } catch (Exception ignored) {
                }
            } else {
                String masked = (String)value;
                Pattern patt = Pattern.compile(regex);
                Matcher m = patt.matcher((String)value);
                while (m.find()) {
                    String text = m.group(1);
                    if (!StringUtil.isBlank(text.trim())) {
                        int position = masked.indexOf(text);
                        if (position != -1) {
                            masked = masked.substring(0, position) + MaskUtil.maskString(text) +
                                     masked.substring(position + text.length());
                        }
                    }
                }
                try {
                    setProperty(object, fieldName[0], masked);
                } catch (Exception ignored) {
                }
            }
        } else {
            String[] modifiedArray = Arrays.copyOfRange(fieldName, 1, fieldName.length);
            if (value instanceof Collection) {
                for (Object obj : (Collection)value) {
                    maskField(obj, modifiedArray, regex);
                }
            }
        }
    }

}
