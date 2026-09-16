document.addEventListener("DOMContentLoaded", function () {

    const commentForm = document.querySelector("#commentForm");
    const commentInput = document.querySelector(".comment-input");
    const commentList = document.querySelector(".comment-list");

    const formatDate = new Intl.DateTimeFormat("ko-KR", {
        month: "numeric",
        day: "numeric"
    });
    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

    const boardId = document.querySelector("#bid").value.trim();

    commentForm.addEventListener("submit", async function (event) {

        // form 기본 submit 동작 방지
        event.preventDefault();


        const content = commentInput.value.trim();

        // 댓글 입력 여부 검사
        if (!content) {
            alert("댓글 내용을 입력해주세요.");
            commentInput.focus();
            return;
        }

        // 현재 테스트용 게시글 번호
        // const boardId = 1;

        const formData = new FormData();
        formData.append("comContent", content);

        try {

            //CSRF 토큰 헤더에 추가
            const response = await fetch(`/api/boards/${boardId}/comments/add`, {
                method: "POST",
                headers: {
                    [csrfHeader]: csrfToken
                },
                body: formData
            });

            if (!response.ok) {
                throw new Error("댓글 등록 실패");
            }
            commentInput.value = ""

            const comment = await response.json();

            // 댓글 등록 성공
            addCommentToScreen(comment);

            console.log("2");
            // 입력창 초기화
            commentInput.value = "";

        }
        catch (error) {

            console.error(error);
            alert("댓글 등록 중 오류가 발생했습니다.");
        }
    });

    function loadComments() {
        fetch(`/api/boards/${boardId}/comments`)
            .then(response => {
                if (!response.ok) {
                    throw new Error("댓글 조회 실패");
                }

                return response.json();
            })
            .then(comments => {

                // console.log("댓글 목록:", comments);

                // comments.forEach(comment => {
                //     addCommentToScreen(comment);
                // });
                renderComments(comments);

            })
            .catch(error => {
                console.error("댓글 조회 오류:", error);
            });
    }


    function renderComments(comments) {

        commentList.innerHTML = "";

        // 부모가 없는 최상위 댓글들
        const rootComments = comments.filter(
            comment => comment.parentCommentId == null
        );

        rootComments.forEach(comment => {
            renderCommentTree(comment, comments, 0);
        });

        setCommentCount();
    }

    function renderCommentTree(comment, comments, depth) {

        // 현재 댓글 출력
        addCommentToScreen(comment, depth);

        // 현재 댓글을 부모로 가진 답글 찾기
        const replies = comments.filter(
            reply => reply.parentCommentId === comment.comId
        );

        // 답글도 똑같이 출력
        replies.forEach(reply => {
            renderCommentTree(reply, comments, depth + 1);
        });
    }

    // function renderComments(comments) {
    //
    //     // 기존 내용 초기화
    //     commentList.innerHTML = "";
    //
    //
    //     // 1. 원댓글만
    //     const parents = comments.filter(
    //         comment => comment.parentCommentId == null
    //     );
    //
    //
    //     parents.forEach(parent => {
    //
    //         // 원댓글 출력
    //         addCommentToScreen(parent);
    //
    //
    //         // 이 댓글에 달린 답글 찾기
    //         const replies = comments.filter(
    //             comment =>
    //                 comment.parentCommentId === parent.comId
    //         );
    //
    //
    //         // 바로 뒤에 답글 출력
    //         replies.forEach(reply => {
    //             addCommentToScreen(reply);
    //         });
    //
    //     });
    //
    //
    //     setCommentCount();
    // }

    function addCommentToScreen(comment, depth=0) {
        const commentItem = document.createElement("div");

        commentItem.classList.add("comment-item");
        commentItem.dataset.commentId = comment.comId;

        const isReply =
            comment.parentCommentId != null;


        if (isReply) {
            commentItem.classList.add("comment-reply");
        }

        const replyTarget = isReply
            ? `<span class="reply-target">@${comment.parentEmplName}</span>`
            : "";

        const actions =
            (comment.mine ? `
                <button type="button" class="comment-edit-btn">
                    수정
                </button>
                <button type="button" class="comment-delete-btn">
                    삭제
                </button>
            ` : "")
            +`
                <button type="button" class="comment-reply-btn">
                    답글
                </button>
            `;


        commentItem.innerHTML = `
            <div class="comment-profile">
                ${comment.emplName.charAt(0)}
            </div>
    
            <div class="comment-body">
    
                <div class="comment-header">
                    <span class="comment-writer">${comment.emplName}</span>
                    <span class="comment-date">
                        ${formatDateTime(comment.createdAt)}
                    </span>
                </div>
                
                <div class="comment-content">
                    ${replyTarget}
                    <span class="comment-text"></span>
                </div>
                
                <div class="comment-actions">
                    ${actions}
                </div>
            </div>
        `;
        //
        // commentItem.querySelector(".comment-content").textContent = comment.comContent;
        commentItem.querySelector(".comment-text").textContent = comment.comContent;

        commentList.appendChild(commentItem);

        setCommentCount();
    }

    commentList.addEventListener("click", async function (event) {

        const commentItem = event.target.closest(".comment-item");

        if (!commentItem) {
            return;
        }

        const commentId = commentItem.dataset.commentId;

        // 수정 버튼
        if (event.target.classList.contains("comment-edit-btn")) {
            editComment(commentItem, commentId);
        }

        // 삭제 버튼
        if (event.target.classList.contains("comment-delete-btn")) {
            await deleteComment(commentItem, commentId);
        }

        // 답글 버튼
        if (event.target.classList.contains("comment-reply-btn")) {
            openReplyInput(commentItem, commentId);
        }

    });

    function editComment(commentItem, commentId) {

        const contentElement =
            commentItem.querySelector(".comment-content");

        const actionsElement =
            commentItem.querySelector(".comment-actions");

        const originalContent = contentElement.textContent.trim();


        // textarea 생성
        const textarea = document.createElement("textarea");

        textarea.classList.add("comment-edit-input");
        textarea.value = originalContent;
        textarea.maxLength = 500;


        // 기존 댓글 내용 대신 textarea 넣기
        contentElement.textContent = "";
        contentElement.appendChild(textarea);


        // 버튼을 저장 / 취소로 변경
        actionsElement.innerHTML = `
            <button type="button" class="comment-save-btn">저장</button>
            <button type="button" class="comment-cancel-btn">취소</button>
        `;

        textarea.focus();


        // 저장
        actionsElement
            .querySelector(".comment-save-btn")
            .addEventListener("click", async function () {

                const newContent = textarea.value.trim();

                if (!newContent) {
                    alert("댓글 내용을 입력해주세요.");
                    textarea.focus();
                    return;
                }

                try {

                    const formData = new FormData();
                    formData.append("comContent", newContent);

                    const response = await fetch(
                        `/api/boards/${commentId}/comments/edit`,
                        {
                            method: "POST",
                            headers: {
                                [csrfHeader]: csrfToken
                            },
                            body: formData
                        }
                    );

                    if (!response.ok) {
                        throw new Error("댓글 수정 실패");
                    }


                    // 수정된 내용 화면 반영
                    contentElement.textContent = newContent;

                    restoreCommentButtons(actionsElement);

                } catch (error) {

                    console.error(error);
                    alert("댓글 수정 중 오류가 발생했습니다.");
                }

            });


        // 취소
        actionsElement
            .querySelector(".comment-cancel-btn")
            .addEventListener("click", function () {

                contentElement.textContent = originalContent;

                restoreCommentButtons(actionsElement);

            });
    }

    function restoreCommentButtons(actionsElement) {

        actionsElement.innerHTML = `
        <button type="button" class="comment-edit-btn">수정</button>
        <button type="button" class="comment-delete-btn">삭제</button>
    `;
    }

    async function deleteComment(commentItem, commentId) {
        if (!confirm("댓글을 삭제하시겠습니까?")) {
            return;
        }

        try {
            const response = await fetch(
                `/api/boards/${commentId}/comments/remove`,
                {
                    method: "POST",
                    headers: {
                        [csrfHeader]: csrfToken
                    },
                }
            );

            if (response.status === 409) {
                alert("답글이 있는 댓글은 삭제할 수 없습니다.");
                return;
            }

            if (!response.ok) {
                throw new Error("댓글 삭제 실패");
            }

            // 화면에서도 제거
            commentItem.remove();

        } catch (error) {
            console.error(error);
            alert("댓글 삭제 중 오류가 발생했습니다.");
        }
        setCommentCount();
    }

    function openReplyInput(commentItem, commentId) {

        // 이미 열려있는 답글창 제거
        document.querySelectorAll(".comment-reply-form")
            .forEach(form => form.remove());


        const replyForm = document.createElement("div");

        replyForm.classList.add("comment-reply-form");

        replyForm.innerHTML = `
        <textarea
            class="comment-reply-input"
            maxlength="200"
            placeholder="답글을 입력해주세요."></textarea>

        <div class="comment-reply-buttons">
            <button type="button"
                    class="comment-reply-submit">
                등록
            </button>

            <button type="button"
                    class="comment-reply-cancel">
                취소
            </button>
        </div>
    `;


        // 댓글의 body 가장 아래에 붙임
        commentItem
            .querySelector(".comment-body")
            .appendChild(replyForm);


        const textarea =
            replyForm.querySelector(".comment-reply-input");

        textarea.focus();


        // 취소
        replyForm
            .querySelector(".comment-reply-cancel")
            .addEventListener("click", function () {

                replyForm.remove();
            });


        // 등록
        replyForm
            .querySelector(".comment-reply-submit")
            .addEventListener("click", async function () {

                const content = textarea.value.trim();

                if (!content) {
                    alert("답글 내용을 입력해주세요.");
                    textarea.focus();
                    return;
                }


                const formData = new FormData();

                formData.append("comContent", content);

                // ★ 이거 하나 때문에 일반댓글 → 답글이 됨
                formData.append("parentCommentId", commentId);


                try {

                    const response = await fetch(
                        `/api/boards/${boardId}/comments/add`,
                        {
                            method: "POST",
                            headers: {
                                [csrfHeader]: csrfToken
                            },
                            body: formData
                        }
                    );


                    if (!response.ok) {
                        throw new Error("답글 등록 실패");
                    }


                    replyForm.remove();

                    // 댓글 전체 다시 불러오기
                    await loadComments();


                } catch (error) {

                    console.error(error);
                    alert("답글 등록 중 오류가 발생했습니다.");
                }
            });
    }
    // function getCurrentDateTime() {
    //
    //     const now = new Date();
    //
    //     const year = now.getFullYear();
    //     const month = String(now.getMonth() + 1).padStart(2, "0");
    //     const day = String(now.getDate()).padStart(2, "0");
    //
    //     const hour = String(now.getHours()).padStart(2, "0");
    //     const minute = String(now.getMinutes()).padStart(2, "0");
    //
    //     return `${year}.${month}.${day} ${hour}:${minute}`;
    // }

    function setCommentCount() {
        const countTag = document.querySelector(".comment-count");
        let countText=document.getElementsByClassName('comment-item').length;

        countTag.textContent=countText.toString();
    }

    loadComments();
});

function formatDateTime(dateTime) {
    if (!dateTime) {
        return '-';
    }

    const date = new Date(dateTime);

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hour = String(date.getHours()).padStart(2, '0');
    const minute = String(date.getMinutes()).padStart(2, '0');

    return `${year}-${month}-${day} ${hour}시${minute}분`;
}