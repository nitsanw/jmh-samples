package psy.lob.saw;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class BaselineBenchmarks {
	int i;
	@Benchmark
	public void noop() {
	}
	
	@Benchmark
	public void increment() {
		i++;
	}
	
    @Benchmark
    public int incrementConsume() {
        return i++;
    }
    
    @Benchmark
    public int consume() {
        return i;
    }
    
    @Benchmark
    public int consumeAdd() {
        return i + 1;
    }
}
