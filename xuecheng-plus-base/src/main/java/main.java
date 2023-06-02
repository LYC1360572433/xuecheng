import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class main {

    public static void main(String[] args) {
        int arr[] = {1,1};
        maxArea(arr);
    }


    public static int maxArea(int[] height) {
        List<Integer> rets = new ArrayList<>();
        int i = 1;
        int j = height.length;
        while (i < j) {
            if (height[i - 1] <= height[j - 1]) {
                int ret = (j - i) * height[i - 1];
                rets.add(ret);
                i++;
            }
            int ret = (j - i) * height[j - 1];
            rets.add(ret);
            j--;
        }
        Collections.sort(rets);
        return rets.get(height.length - 2);
    }
}
