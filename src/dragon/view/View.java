package dragon.view;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

import dragon.Dragon;

public interface View {

    InputStream toView(Stream<Dragon> stream);

    Stream<Dragon> fromView(OutputStream stream);

}
