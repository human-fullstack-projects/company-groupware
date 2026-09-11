

const saveButton = document.getElementById("saveButton");
const deleteButton = document.getElementById("deleteButton");
const approveButton = document.getElementById("approveButton");
const rejectButton = document.getElementById("rejectButton");

const approvalPage = document.getElementById("approvalPage");//문서 아이디 저장해둔 위치
const documentId = Number(approvalPage.dataset.documentId);

/*
 * 수정
 */
saveButton?.addEventListener("click",async () => {
        const title = document.getElementById("documentTitle").value.trim();
        const content = document.getElementById("documentContent").value.trim();

        if (!title) {
            alert("제목을 입력해주세요.");
            return;
        }

        if (!content) {
            alert("내용을 입력해주세요.");
            return;
        }

        await request(
            `/approvals/api/${documentId}`,
            {
                method: "PUT",
                body: JSON.stringify({
                    title,
                    content,
                    approvalLineId: null
                })
            }
        );

        alert("수정되었습니다.");

        location.reload();
    }
);


/*
 * 삭제
 */
deleteButton?.addEventListener(
    "click",
    async () => {

        if (!confirm("문서를 삭제하시겠습니까?")) {
            return;
        }

        await request(`/approvals/api/${documentId}`,{method: "DELETE"});

        alert("삭제되었습니다.");

        location.href = "/approvals";
    }
);


/*
 * 승인
 */
approveButton?.addEventListener(
    "click",
    async () => {
        if (!confirm("이 문서를 승인하시겠습니까?")) {
            return;
        }

        await request(
            `/approvals/api/${documentId}/approve`,
            {
                method: "POST",
                body: JSON.stringify({comment: null})
            }
        );

        alert("승인되었습니다.");

        location.href = "/approvals";
    }
);


/*
 * 반려
 */
rejectButton?.addEventListener("click",async () => {
        const comment = prompt("반려 사유를 입력해주세요.");
        if (comment === null) { return;}

        await request(
            `/approvals/api/${documentId}/reject`,
            {
                method: "POST",
                body: JSON.stringify({comment: comment})
            }
        );

        alert("반려되었습니다.");
        location.href = "/approvals";
    }
);


async function request(url,options = {}){
    const csrfToken = document.querySelector('meta[name="_csrf"]');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]');


    options.headers = {
        "Content-Type":"application/json",
        ...(options.headers || {})
    };


    if (csrfToken && csrfHeader) {
        options.headers[csrfHeader.content] = csrfToken.content;
    }

    const response = await fetch(url,options);

    if (!response.ok) {
        const message = await response.text();
        console.error(message);
        alert("요청 처리 중 오류가 발생했습니다.");
        throw new Error(message);
    }

    if (response.status === 204) {return null;}

    return await response.json();
}