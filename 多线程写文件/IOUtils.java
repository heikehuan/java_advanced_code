package com.ptmind.ptfence.util;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

/**
 * Created by ptmind on 2017/5/15.
 */
public class IOUtils {

    private static String CharsetName = "UTF-8";

    /**
     * 若文件不存在会创建新的文件
     *
     * @param fileName
     * @param data
     * @return
     * @throws IOException
     */
    public static long write(String fileName, String data) throws IOException {

        File file = new File(fileName);
        if (file.exists())
            file.delete();

        file.createNewFile();

        return append(file, 0, data);
    }

    /**
     * 若文件不存在会异常
     *
     * @param fileName
     * @param data
     * @return
     * @throws IOException
     */
    public static long append(String fileName, String data) throws IOException {

        File file = new File(fileName);
        if (!file.exists())
            throw new IOException(fileName + " not exists.");

        return append(file, file.length(), data);
    }

    private static long append(File file, long pos, String data) throws IOException {

        WriteWorker worker = new WriteWorker(file.getPath(), pos, data.getBytes(CharsetName), 0, data.length());
        ForkJoinPool executor = ForkJoinPoolHolder.getForkJoinPoolHolder();
        ForkJoinTask<Integer> task = executor.submit(worker);

        try {
            return task.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static class WriteWorker extends RecursiveTask<Integer> {

        //阈值
        private final static int THRESHOLD = 4096;

        private final String fileName;

        private final long pos;

        //数组的开始和结束位置
        private final int start;
        private final int end;

        private final byte[] data;

        public WriteWorker(String fileName, long pos, byte[] data, int start, int end) {
            this.fileName = fileName;
            this.pos = pos;
            this.start = start;
            this.end = end;
            this.data = data;
        }

        @Override
        protected Integer compute() {
            int cnt = 0;

            int len = end - start;
            if (len <= THRESHOLD) {
                Thread curThread = Thread.currentThread();
//				System.out.println(curThread.getId() + ":" + "start:" + start + ", end:" + end + ", pos:" + pos + ", len:" + len);
                try {
                    RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
                    raf.seek(pos);

                    FileChannel wc = raf.getChannel();

                    ByteBuffer buf = ByteBuffer.wrap(data, start, len);
                    int sum = 0;
                    while (buf.hasRemaining()) {
                        sum += wc.write(buf);
                    }

                    cnt = sum;

                    wc.close();
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            } else {
                cnt = split();
            }

            return cnt;
        }

        private int split() {
            int cnt = data.length / THRESHOLD;
            int remainder = data.length % THRESHOLD;

            WriteWorker[] workers;
            if (remainder != 0)
                workers = new WriteWorker[cnt + 1];
            else
                workers = new WriteWorker[cnt];

            for (int i = 0; i < cnt; i++) {
                int start = i * THRESHOLD;
                int end = (i + 1) * THRESHOLD;
                long pos = this.pos + start;
                workers[i] = new WriteWorker(fileName, pos, data, start, end);
            }

            if (remainder != 0) {
                int start = data.length - remainder;
                int end = data.length;
                long pos = this.pos + start;
                workers[workers.length - 1] = new WriteWorker(fileName, pos, data, start, end);
            }

            for (int i = 0; i < workers.length; i++) {
                workers[i].fork();
            }

            int sum = 0;
            for (int i = 0; i < workers.length; i++) {
                sum += workers[i].join();
            }

            return sum;
        }
    }

    private static class ForkJoinPoolHolder {
        private final static ForkJoinPool executor = new ForkJoinPool();

        public static ForkJoinPool getForkJoinPoolHolder() {
            return executor;
        }
    }

//    private static void test2() throws Exception {
//        String url = "C:\\Users\\ptmind\\Desktop\\java题目.txt";
//
//        File file = new File(url);
//        if (file.exists())
//            System.out.println("src file length:" + file.length());
//
//        FileInputStream fis = new FileInputStream(url);
//
//        StringBuilder builder = new StringBuilder();
//        byte[] bytes = new byte[1024];
//        int len;
//        while ((len = fis.read(bytes)) != -1) {
//            builder.append(new String(bytes,0, len));
//        }
//        builder.trimToSize();
//        System.out.println(builder.toString());
//
//        long start = System.currentTimeMillis();
//        long cnt = IOUtils.write("d:\\dump.txt", fis);
//        long end = System.currentTimeMillis();
//
//        System.out.println("========>" + builder.toString().getBytes().length);
//        System.out.println("==>" + cnt);
//
//        fis.close();
//        System.out.println("共花费:" + (end - start) + "ms");
//    }

}
