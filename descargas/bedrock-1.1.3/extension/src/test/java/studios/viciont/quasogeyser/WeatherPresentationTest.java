package studios.viciont.quasogeyser;

import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.java.level.JavaGameEventTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public final class WeatherPresentationTest {
    public static void main(String[] args) {
        var session=mock(GeyserSession.class);
        float[] raw={1,1};
        when(session.getRainStrength()).thenAnswer(c->raw[0]);
        when(session.getThunderStrength()).thenAnswer(c->raw[1]);
        doAnswer(c->{raw[0]=c.getArgument(0);return null;}).when(session).updateRain(anyFloat());
        doAnswer(c->{raw[1]=c.getArgument(0);return null;}).when(session).updateThunder(anyFloat());
        doAnswer(c->{c.getArgument(0,Runnable.class).run();return null;}).when(session).executeInEventLoop(any());
        List<LevelEventPacket> sent=new ArrayList<>();
        doAnswer(c->{sent.add(c.getArgument(0));return null;}).when(session).sendUpstreamPacket(any(LevelEventPacket.class));
        var weather=new WeatherPresentation();
        var delegate=new JavaGameEventTranslator();
        int previous=65535;
        for(int i=1;i<=20;i++) {
            weather.strength(session,i);
            check(last(sent).getData()<previous,"Oscurecimiento disminuye gradualmente");
            previous=last(sent).getData();
        }
        check(raw[0]==1 && raw[1]==1,"Estado real de Geyser intacto");
        check(sent.get(sent.size()-2).getType()==LevelEvent.START_RAINING,"La lluvia sigue activa");
        near(sent.get(sent.size()-2).getData(),.30f,"Lluvia suave");
        near(last(sent).getData(),.08f,"Oscurecimiento de tormenta reducido");
        int count=sent.size();weather.strength(session,20);
        check(sent.size()==count,"No reenvía un paso idéntico");
        weather.translateWeather(session,new ClientboundGameEventPacket(GameEvent.RAIN_LEVEL_CHANGE,new RainStrengthValue(.5f)),delegate);
        check(raw[0]==.5f,"El traductor original recibe la actualización");
        near(sent.get(sent.size()-2).getData(),.15f,"La actualización no restaura la lluvia intensa");
        weather.translateWeather(session,new ClientboundGameEventPacket(GameEvent.THUNDER_LEVEL_CHANGE,new ThunderStrengthValue(.4f)),delegate);
        near(last(sent).getData(),.032f,"Actualización de tormenta atenuada");
        weather.strength(session,0);
        near(sent.get(sent.size()-2).getData(),.5f,"Restaura la lluvia actual, no una copia antigua");
        near(last(sent).getData(),.4f,"Restaura la tormenta actual");
        count=sent.size();
        weather.translateWeather(session,new ClientboundGameEventPacket(GameEvent.RAIN_LEVEL_CHANGE,new RainStrengthValue(.8f)),delegate);
        check(raw[0]==.8f && sent.size()==count,"Fuera de BloodMoon sólo traduce Geyser");
        raw[0]=0;raw[1]=1;weather.strength(session,20);
        check(last(sent).getType()==LevelEvent.STOP_THUNDERSTORM,"No provoca lluvia al enviar tormenta en tiempo seco");
        check(last(sent).getData()==0,"No hay intensidad residual en seco");
        raw[0]=1;raw[1]=1;weather.stop();
        near(last(sent).getData(),1,"Apagar restaura el clima");
        weather.strength(session,20);weather.retain(Set.of());
        count=sent.size();weather.strength(session,0);
        check(count==sent.size(),"No retiene sesiones cerradas");
        System.out.println("OK: clima real intacto, fade, paquetes nuevos, restauración, sesión cerrada y sin lluvia inventada.");
    }
    static LevelEventPacket last(List<LevelEventPacket> p) { return p.getLast(); }
    static void near(int actual,float expected,String message) { check(Math.abs(actual-expected*65535)<1.1f,message); }
    static void check(boolean b,String message) { if(!b) throw new AssertionError(message); }
}
