package edu.vanderbilt.imagecrawler.crawlers;

import static edu.vanderbilt.imagecrawler.crawlers.Crawler.Type.IMAGE;
import static edu.vanderbilt.imagecrawler.crawlers.Crawler.Type.PAGE;

import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import edu.vanderbilt.imagecrawler.platform.Cache;
import edu.vanderbilt.imagecrawler.utils.BlockingTask;
import edu.vanderbilt.imagecrawler.utils.Image;
import edu.vanderbilt.imagecrawler.web.WebPageElement;

/**
 * This ImageCrawler implementation uses Java parallel streams to
 * perform an "image crawl" starting from a root Uri.  Images from
 * HTML page reachable from the root Uri are downloaded from a remote
 * web server or the local file system and the results are stored in
 * files that can be displayed to the user.
 * <p>
 * This implementation is a straightforward revision of the
 * SequentialStreamsCrawler.java file, with minuscule changes to use
 * parallel streams instead of sequential streams.
 */
@SuppressWarnings("unchecked")
public class ParallelStreamsCrawler // Loaded via reflection
    extends ImageCrawler {
    /**
     * Recursively crawls the given page and returns the total
     * number of processed images.
     *
     * @param pageUri The URI that's being crawled at this point
     * @param depth   The current depth of the recursive processing
     * @return The count of the number of images processed at this depth
     */
    @Override
    protected int performCrawl(String pageUri, int depth) {
        // Throw an exception if the the stop crawl flag has been set.
        throwExceptionIfCancelled();

        log("[" + Thread.currentThread().getName()
            + "] Crawling " + pageUri + " (depth " + depth + ")");

        // Create and use a Java sequential stream to:
        // 1. Use a factory method to create a one-element stream
        //    containing just the pageUri.
        // 2. Use an intermediate operation to filter out pageUri if
        //    it exceeds max depth or was already visited.
        // 3. Use an intermediate operation to call the crawlPage()
        //    method and return the total number of processed images.
        // 4. Use a terminal operation to get the total number of
        //    processed images from the one-element stream.

        // TODO -- you fill in here replacing this statement with your solution.
        return Stream.of(pageUri)
                .filter(uri -> depth <= mMaxDepth && mUniqueUris.add(uri))
                .mapToInt(uri -> crawlPage(uri, depth))
                .findFirst()
                .orElse(0);
    }

    /**
     * Perform a crawl starting at {@code pageUri} and return the sum
     * of all images processed during the crawl.
     *
     * @param pageUri The page uri to crawl
     * @param depth   The current depth of the recursive processing
     * @return The count of the number of images processed
     */
    protected int crawlPage(String pageUri, int depth) {
        log("[" + Thread.currentThread().getName()
            + "] Crawling " + pageUri + " (depth " + depth + ")");

        // Create and use a Java sequential stream to:
        // 1. Call a Stream factory method to create a one-element
        //    stream containing just the pageUri.
        // 2. Get the HTML page associated with pageUri using the
        //    ImageCrawler.callInManagedBlocker() method to expand the
        //    Java common fork-join pool.
        // 3. Filter out a missing (null) HTML page.
        // 4. Call processPage() to process images encountered during
        //    the crawl.
        // 5. Use a terminal operation to get the total number of
        //    processed images from the one-element stream.
        //
        // Return the count of all the images downloaded, processed, and
        // stored.
        // TODO -- you fill in here replacing this statement with your
        // solution.
        return Stream.of(pageUri)
                .map(uri -> callInManagedBlocker(() ->
                        mWebPageCrawler.getPage(uri)))
                .filter(Objects::nonNull)
                .mapToInt(page -> processPage(page, depth))
                .findFirst()
                .orElse(0);
    }

    /**
     * Use a Java parallel stream to (1) download and process images
     * on this page via processImage(), (2) recursively crawl other
     * hyperlinks accessible from this page via performCrawl(), and
     * (3) return the sum of all images processed during the crawl.
     *
     * @param page  The page containing HTML
     * @param depth The current depth of the recursive processing
     * @return The count of the number of images processed
     */
    protected int processPage(Crawler.Page page,
                              int depth) {
        // Perform the following steps:
        // 1. Get a sequential stream containing all the image/page
        //    elements on this page.
        // 2. Convert the sequential stream to a parallel stream.
        // 3. Map each web element into the count of images produced
        //    by either processing an image or by crawling a page.
        // 4. Sum all the results together.

        // Return a count of of all images processed on/from this page.
        // TODO -- you fill in here replacing this statement with your
        // solution.
        return page
                .getPageElementsAsStream(IMAGE, PAGE)
                .parallel()
                .mapToInt(e -> e.getType() == IMAGE ?
                        processImage(e.getURL()) :
                        performCrawl(e.getUrl(), depth + 1))
                .sum();
    }

    /**
     * Process an image by applying any transformations that have not
     * already been applied and cached.
     *
     * @param url A {@link URL} to an image to download
     * @return The count of transformed images
     */
    protected int processImage(URL url) {
        // Create and use a Java sequential stream to:
        // 1. Use a factory method to create a one-element stream
        //    containing just the pageUri.
        // 2. Get or download the image from the given url in
        //    conjunction with the managedBlockerDownloadImage method
        //    reference.
        // 3. Filter out a missing (null) page upon failure.
        // 4. Transform the image and return a count of the number of
        //    transforms applied.
        // 5. Sum the number of transformed images.

        // Return the count of transformed images.
        // TODO -- you fill in here replacing this statement with your
        // solution.
        return Stream.of(url)
                .map(imageUrl -> getOrDownloadImage(imageUrl,
                        this::managedBlockerDownloadImage))
                .filter(Objects::nonNull)
                .mapToInt(this::transformImage)
                .sum();
    }

    /**
     * Route the request to transform an {@link Image} to either a
     * local or remote transformer.
     *
     * @param image The {@link Image} to transform
     * @return The count of the non-null transformed images
     */
    protected int transformImage(Image image) {
        // Check a flag to determine if transforms should be run
        // remotely on a remote server using microservices or locally.
        return runRemoteTransforms()
            ? transformImageRemotely(image)
            : transformImageLocally(image);
    }

    /**
     * Locally applies the current set of crawler transforms on the
     * passed {@link Image} and returns a count of all successfully
     * transformed images.
     *
     * @param image The {@link Image} to transform locally
     * @return The count of the non-null transformed images
     */
    protected int transformImageLocally(Image image) {
        // Create and use a Java parallel stream as follows:
        // 1. Convert the List of transforms into a parallel stream.
        // 2. Attempt to create a new cache item for each image,
        //    filtering out any image that has already been locally
        //    cached.
        // 3. Apply each transform to the original image to produce a
        //    transformed image.
        // 4. Filter out any null images that weren't transformed.
        // 5. Count the number of non-null images that were transformed.

        // TODO -- you fill in here replacing this statement with your solution.
        return (int) mTransforms
                .parallelStream()
                .filter(transform -> createNewCacheItem(image, transform))
                .map(transform -> applyTransform(transform, image))
                .filter(Objects::nonNull)
                .count();
    }

    /**
     * Calls remote server to perform transforms on the passed {@link
     * Image} and return a count of all successfully transformed
     * images.
     *
     * @param image The {@link Image} to transform remotely
     * @return The number of transformed images
     */
    protected int transformImageRemotely(Image image) {
        // Perform all transform operations and then return the number
        // of transformed images as follows:
        // 1. Call getRemoteDataSource() to get a proxy to the server.
        // 2. Call a RemoteDataSource helper method to apply all
        //    transforms on the image remotely.
        // 3. Convert the List of resulting transformed images
        //    into a parallel stream.
        // 4. Call createImage() to convert the received
        //    TransformedImage to a locally cached Image.
        // 5. Skip null images.
        // 6. Return the count of the number of transformed images.

        // TODO -- you fill in here replacing this statement with your
        // solution.
        return (int) getRemoteDataSource()
                .applyTransforms(this, image, getTransformNames(), true)
                .parallelStream()
                .map(transformedImage -> createImage(image, transformedImage))
                .filter(Objects::nonNull)
                .count();
    }

    /**
     * Use {@link BlockingTask} to encapsulate the {@link Supplier} so
     * it runs in the context of the Java common fork-join pool {@link
     * ForkJoinPool.ManagedBlocker} mechanism.
     *
     * @param supplier The {@link Supplier} to call
     * @return The result of calling the {@link Supplier} in the
     * context of the Java common fork-join pool {@link
     * ForkJoinPool.ManagedBlocker} mechanism
     */
    @Override
    protected <T> T callInManagedBlocker(Supplier<T> supplier) {
        // Use BlockingTask.callInManagedBlock() to run the supplier
        // as a ManagedBlocker.
        // TODO -- you fill in here replacing null with your solution.
        return BlockingTask.callInManagedBlock(supplier);
    }

    /**
     * Convert {@link Cache.Item} to an Image by downloading it.
     * This call ensures the common fork/join thread pool is expanded
     * to handle the blocking image download.
     *
     * @param item The {@link Cache.Item} to download
     * @return The downloaded {@link Image}
     */
    @Override
    protected Image managedBlockerDownloadImage(Cache.Item item) {
        // Use callInManagedBlocker() and downloadImage() to download
        // the item in a ManagedBlocker.
        // TODO -- you fill in here replacing null with your solution.
        return callInManagedBlocker(() -> downloadImage(item));
    }
}
