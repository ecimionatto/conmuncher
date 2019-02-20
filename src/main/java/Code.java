public class Code {

    final String clientIp;

    public String getClientIp() {
        return clientIp;
    }

    public String getCode() {
        return code;
    }

    public boolean isPrinted() {
        return printed;
    }

    final String code;
    final boolean printed;

    public Code(final String clientIp, final String code, final boolean printed) {
        this.clientIp = clientIp;
        this.code = code;
        this.printed = printed;
    }
}
