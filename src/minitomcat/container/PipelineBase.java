package minitomcat.container;

import minitomcat.http.Request;
import minitomcat.http.Response;

/**
 * Pipeline 的默认实现：仅包含一个 BasicValve，invoke 时直接调用 BasicValve。
 */
public class PipelineBase implements Pipeline {
    private Valve basic;

    @Override
    public void invoke(Request request, Response response) throws Exception {
        if (basic != null) {
            basic.invoke(request, response, null);
        }
    }

    @Override
    public void setBasic(Valve valve) {
        this.basic = valve;
    }

    @Override
    public Valve getBasic() {
        return basic;
    }
}
